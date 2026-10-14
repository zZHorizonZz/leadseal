package dev.horizon.leadseal.protobuf;

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.Check;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.PrimaryKey;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the table messages of protobuf files into a schema.
 *
 * <p>
 * A message is a table if it has the {@code (leadseal.v1.table)} option. Each field is a column, unless its
 * {@code (leadseal.v1.column)} option has {@code ignore} set. The full message name is the table id and the field
 * number is the column id, so renaming a message or a field renames its table or column and keeps the rows. The
 * leading comments of messages and fields are the comments of their tables and columns.
 *
 * <p>
 * The column type follows from the field type:
 *
 * <ul>
 * <li>{@code bool}: {@code BOOLEAN}.</li>
 * <li>{@code int32}, {@code sint32}, {@code sfixed32}: {@code INT}.</li>
 * <li>{@code uint32}, {@code fixed32}, {@code int64}, {@code uint64}, {@code sint64}, {@code fixed64},
 * {@code sfixed64}: {@code BIGINT}.</li>
 * <li>{@code float}: {@code REAL}. {@code double}: {@code DOUBLE}.</li>
 * <li>{@code string} and enums: {@code TEXT}. An enum column stores the value name.</li>
 * <li>{@code bytes}: {@code BYTES}.</li>
 * <li>{@code google.protobuf.Timestamp}: {@code TIMESTAMP}. {@code google.type.Date}: {@code DATE}.
 * {@code google.type.TimeOfDay}: {@code TIME}. {@code google.type.Decimal}: {@code DECIMAL}.</li>
 * <li>Wrapper messages such as {@code google.protobuf.Int64Value}: the type of the wrapped value.</li>
 * <li>Other messages, repeated fields and maps: {@code JSON}.</li>
 * </ul>
 *
 * <p>
 * A column accepts {@code NULL} if its field has presence, for example a field with the {@code optional} label or a
 * message field, and for repeated fields and maps. The {@code nullable} option overrides this.
 *
 * <p>
 * The {@code type} of the column option sets another column type and its options, such as
 * {@code text: {length: 40}} or {@code decimal: {precision: 12, scale: 2}}. A field can only have a type that holds
 * all its values. A {@code string} field can be {@code uuid}, {@code json}, {@code decimal}, {@code date},
 * {@code time} or {@code timestamp}, a {@code bytes} field can be {@code uuid}, an enum field can be {@code integer}
 * to store the value number, and 32-bit numbers can be 64-bit.
 */
public final class ProtoSchema {

  private static final Map<String, ColumnType> MESSAGE_TYPES = Map.ofEntries(
      Map.entry("google.protobuf.Timestamp", ColumnType.TIMESTAMP),
      Map.entry("google.protobuf.BoolValue", ColumnType.BOOLEAN),
      Map.entry("google.protobuf.Int32Value", ColumnType.INT),
      Map.entry("google.protobuf.UInt32Value", ColumnType.BIGINT),
      Map.entry("google.protobuf.Int64Value", ColumnType.BIGINT),
      Map.entry("google.protobuf.UInt64Value", ColumnType.BIGINT),
      Map.entry("google.protobuf.FloatValue", ColumnType.REAL),
      Map.entry("google.protobuf.DoubleValue", ColumnType.DOUBLE),
      Map.entry("google.protobuf.StringValue", ColumnType.TEXT),
      Map.entry("google.protobuf.BytesValue", ColumnType.BYTES),
      Map.entry("google.type.Date", ColumnType.DATE),
      Map.entry("google.type.TimeOfDay", ColumnType.TIME),
      Map.entry("google.type.Decimal", ColumnType.DECIMAL));
  private static final Map<ColumnType, Set<ColumnType>> WIDER = Map.of(
      ColumnType.TEXT, Set.of(ColumnType.UUID, ColumnType.JSON, ColumnType.DECIMAL, ColumnType.DATE, ColumnType.TIME,
          ColumnType.TIMESTAMP),
      ColumnType.BYTES, Set.of(ColumnType.UUID),
      ColumnType.INT, Set.of(ColumnType.BIGINT),
      ColumnType.REAL, Set.of(ColumnType.DOUBLE));
  private static final Set<ColumnType> ENUM_TYPES = Set.of(ColumnType.INT, ColumnType.BIGINT);
  private static final int MESSAGE_TYPE = 4;
  private static final int NESTED_TYPE = 3;
  private static final int FIELD = 2;

  private final Map<FileDescriptor, Map<List<Integer>, String>> comments = new HashMap<>();

  /**
   * Reads the table messages of files, including their nested messages.
   *
   * @param files the files
   * @return the schema
   * @throws SchemaException if an option is invalid, or the tables do not form a valid schema
   */
  public Schema read(List<FileDescriptor> files) {
    Map<String, Descriptor> messages = new LinkedHashMap<>();
    for (FileDescriptor file : files) {
      for (Descriptor message : file.getMessageTypes()) {
        collect(message, messages);
      }
    }
    List<Table> tables = new ArrayList<>();
    for (Descriptor message : messages.values()) {
      tables.add(table(message, messages));
    }
    return new Schema(tables);
  }

  private static void collect(Descriptor message, Map<String, Descriptor> messages) {
    if (message.getOptions().hasExtension(OptionsProto.table)) {
      messages.put(message.getFullName(), message);
    }
    for (Descriptor nested : message.getNestedTypes()) {
      collect(nested, messages);
    }
  }

  private Table table(Descriptor message, Map<String, Descriptor> messages) {
    TableOptions options = message.getOptions().getExtension(OptionsProto.table);
    String name = tableName(message);
    List<Column> columns = new ArrayList<>();
    List<String> primaryKey = new ArrayList<>();
    List<Index> indexes = new ArrayList<>();
    List<ForeignKey> foreignKeys = new ArrayList<>();
    List<Check> checks = new ArrayList<>();
    for (FieldDescriptor field : message.getFields()) {
      ColumnOptions column = field.getOptions().getExtension(OptionsProto.column);
      if (column.getIgnore()) {
        continue;
      }
      Column definition = column(field, column);
      columns.add(definition);
      if (column.getPrimaryKey()) {
        primaryKey.add(definition.name());
      }
      if (column.getUnique() || column.getIndex()) {
        indexes.add(Index.of(name, List.of(definition.name()), column.getUnique()));
      }
      if (column.hasForeignKey()) {
        foreignKeys.add(foreignKey(name, field, definition, column.getForeignKey(), messages));
      }
      if (!column.getCheck().isEmpty()) {
        checks.add(Check.of(name, column.getCheck()));
      }
    }
    for (IndexOptions index : options.getIndexList()) {
      List<String> indexed = index.getFieldsList().stream().map(field -> columnName(message, field)).toList();
      Index definition = index.getName().isEmpty() ? Index.of(name, indexed, index.getUnique())
          : new Index(index.getName(), indexed, index.getUnique());
      indexes.add(definition.withWhere(index.getWhere())
          .withDescending(index.getDescendingList().stream().map(field -> columnName(message, field)).toList()));
    }
    for (CheckOptions check : options.getCheckList()) {
      checks.add(check.getName().isEmpty() ? Check.of(name, check.getExpression())
          : new Check(check.getName(), check.getExpression()));
    }
    for (TableForeignKeyOptions key : options.getForeignKeyList()) {
      foreignKeys.add(foreignKey(name, message, key, messages));
    }
    if (primaryKey.isEmpty()) {
      throw new SchemaException("The table message " + message.getFullName() + " has no primary key. Set"
          + " (leadseal.v1.column).primary_key on its key fields.");
    }
    return new Table(message.getFullName(), name, options.getSchema(), columns, PrimaryKey.of(name, primaryKey),
        indexes, foreignKeys, checks, comment(message.getFile(), path(message)));
  }

  private Column column(FieldDescriptor field, ColumnOptions options) {
    ColumnType natural = defaultType(field);
    ColumnType type = natural;
    Integer length = null;
    Integer precision = null;
    Integer scale = null;
    Default value = null;
    boolean identity = false;
    Map<String, String> collations = Map.of();
    switch (options.getTypeCase()) {
      case TYPE_NOT_SET -> {
      }
      case BOOLEAN -> {
        type = ColumnType.BOOLEAN;
        value = options.getBoolean().hasDefaultValue() ? Default.value(String.valueOf(options.getBoolean()
            .getDefaultValue())) : null;
      }
      case INTEGER -> {
        IntegerColumn integer = options.getInteger();
        type = switch (integer.getFormat()) {
          case FORMAT_INT32 -> ColumnType.INT;
          case FORMAT_INT64 -> ColumnType.BIGINT;
          case FORMAT_UNSPECIFIED, UNRECOGNIZED -> natural == ColumnType.BIGINT ? ColumnType.BIGINT : ColumnType.INT;
        };
        value = integer.hasDefaultValue() ? Default.value(String.valueOf(integer.getDefaultValue())) : null;
        identity = integer.getIdentity();
      }
      case NUMBER -> {
        NumberColumn number = options.getNumber();
        type = switch (number.getFormat()) {
          case FORMAT_FLOAT -> ColumnType.REAL;
          case FORMAT_DOUBLE -> ColumnType.DOUBLE;
          case FORMAT_UNSPECIFIED, UNRECOGNIZED -> natural == ColumnType.REAL ? ColumnType.REAL : ColumnType.DOUBLE;
        };
        value = number.hasDefaultValue() ? Default.value(number(field, number.getDefaultValue())) : null;
      }
      case DECIMAL -> {
        DecimalColumn decimal = options.getDecimal();
        type = ColumnType.DECIMAL;
        precision = decimal.getPrecision() == 0 ? null : decimal.getPrecision();
        scale = decimal.getScale();
        value = decimal.hasDefaultValue() ? Default.value(decimal.getDefaultValue()) : null;
      }
      case TEXT -> {
        TextColumn text = options.getText();
        type = ColumnType.TEXT;
        length = text.getLength() == 0 ? null : text.getLength();
        value = text.hasDefaultValue() ? Default.value(text.getDefaultValue()) : null;
        collations = text.getCollationMap();
      }
      case BYTES -> {
        BytesColumn bytes = options.getBytes();
        type = ColumnType.BYTES;
        length = bytes.getLength() == 0 ? null : bytes.getLength();
        value = bytes.hasDefaultValue() ? Default.value(HexFormat.of().formatHex(bytes.getDefaultValue()
            .toByteArray())) : null;
      }
      case UUID -> {
        UuidColumn uuid = options.getUuid();
        type = ColumnType.UUID;
        value = generated(field, uuid.hasDefaultValue() ? uuid.getDefaultValue() : null, uuid.getDefaultRandom(),
            Default.randomUuid());
      }
      case TIMESTAMP -> {
        TimestampColumn timestamp = options.getTimestamp();
        type = ColumnType.TIMESTAMP;
        String fixed = timestamp.hasDefaultValue() ? Instant.ofEpochSecond(timestamp.getDefaultValue().getSeconds(),
            timestamp.getDefaultValue().getNanos()).toString() : null;
        value = generated(field, fixed, timestamp.getDefaultNow(), Default.now());
      }
      case DATE -> {
        DateColumn date = options.getDate();
        type = ColumnType.DATE;
        value = generated(field, date.hasDefaultValue() ? date.getDefaultValue() : null, date.getDefaultToday(),
            Default.now());
      }
      case TIME -> {
        type = ColumnType.TIME;
        value = options.getTime().hasDefaultValue() ? Default.value(options.getTime().getDefaultValue()) : null;
      }
      case JSON -> {
        type = ColumnType.JSON;
        value = options.getJson().hasDefaultValue() ? Default.value(options.getJson().getDefaultValue()) : null;
      }
    }
    if (type != natural && !allowed(field, natural).contains(type)) {
      throw new SchemaException("The field " + field.getFullName() + " cannot have the column type " + type + ", because its"
          + " values are " + natural + ".");
    }
    if (value != null && enumField(field)) {
      checkEnum(field, type, value.value());
    }
    boolean nullable = options.hasNullable() ? options.getNullable()
        : field.isRepeated() || field.hasPresence() && !field.isRequired();
    List<Integer> path = new ArrayList<>(path(field.getContainingType()));
    path.addAll(List.of(FIELD, field.getIndex()));
    return new Column(String.valueOf(field.getNumber()), columnName(field, options), type, length, precision,
        precision == null ? null : scale, nullable, value, identity, collations, comment(field.getFile(), path));
  }

  /** Returns the column types other than its default that a field can have. */
  private static Set<ColumnType> allowed(FieldDescriptor field, ColumnType natural) {
    if (enumField(field)) {
      return ENUM_TYPES;
    }
    if (field.isRepeated() || field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
      return Set.of();
    }
    return WIDER.getOrDefault(natural, Set.of());
  }

  private static boolean enumField(FieldDescriptor field) {
    return field.getJavaType() == FieldDescriptor.JavaType.ENUM && !field.isRepeated();
  }

  private static String number(FieldDescriptor field, double value) {
    if (!Double.isFinite(value)) {
      throw new SchemaException("The default of the field " + field.getFullName() + " must be a finite number.");
    }
    return BigDecimal.valueOf(value).toPlainString();
  }

  /** Returns a fixed default, a generated default, or none. */
  private static Default generated(FieldDescriptor field, String fixed, boolean generate, Default generated) {
    if (fixed != null && generate) {
      throw new SchemaException("The field " + field.getFullName() + " has both a default_value and a generated"
          + " default. Set one of them.");
    }
    return fixed != null ? Default.value(fixed) : generate ? generated : null;
  }

  private static void checkEnum(FieldDescriptor field, ColumnType type, String value) {
    boolean known;
    if (type == ColumnType.TEXT) {
      known = field.getEnumType().findValueByName(value) != null;
    } else {
      try {
        known = field.getEnumType().findValueByNumber(Integer.parseInt(value)) != null;
      } catch (NumberFormatException notANumber) {
        known = false;
      }
    }
    if (!known) {
      throw new SchemaException("The default '" + value + "' of the field " + field.getFullName() + " is not a value"
          + " of " + field.getEnumType().getFullName() + ". Use a value " + (type == ColumnType.TEXT ? "name"
              : "number")
          + ".");
    }
  }

  private static ColumnType defaultType(FieldDescriptor field) {
    if (field.isRepeated()) {
      return ColumnType.JSON;
    }
    return switch (field.getJavaType()) {
      case BOOLEAN -> ColumnType.BOOLEAN;
      case INT -> unsigned32(field) ? ColumnType.BIGINT : ColumnType.INT;
      case LONG -> ColumnType.BIGINT;
      case FLOAT -> ColumnType.REAL;
      case DOUBLE -> ColumnType.DOUBLE;
      case STRING, ENUM -> ColumnType.TEXT;
      case BYTE_STRING -> ColumnType.BYTES;
      case MESSAGE -> MESSAGE_TYPES.getOrDefault(field.getMessageType().getFullName(), ColumnType.JSON);
    };
  }

  private static boolean unsigned32(FieldDescriptor field) {
    return field.getType() == FieldDescriptor.Type.UINT32 || field.getType() == FieldDescriptor.Type.FIXED32;
  }

  private static ForeignKey foreignKey(String table, FieldDescriptor field, Column column, ForeignKeyOptions options,
      Map<String, Descriptor> messages) {
    Descriptor target = target(field.getFullName(), options.getMessage(), messages);
    String referenced;
    if (options.getField().isEmpty()) {
      List<String> keys = primaryKey(target);
      if (keys.size() != 1) {
        throw new SchemaException("The foreign key of " + field.getFullName() + " needs a field, because "
            + target.getFullName() + " has " + keys.size() + " primary key fields.");
      }
      referenced = keys.getFirst();
    } else {
      referenced = columnName(target, options.getField());
    }
    ForeignKey key = ForeignKey.of(table, List.of(column.name()), tableName(target), List.of(referenced),
        action(options.getOnDelete()));
    return named(key, options.getName()).withOnUpdate(action(options.getOnUpdate()));
  }

  private static ForeignKey foreignKey(String table, Descriptor message, TableForeignKeyOptions options,
      Map<String, Descriptor> messages) {
    Descriptor target = target(message.getFullName(), options.getMessage(), messages);
    List<String> columns = options.getFieldsList().stream().map(field -> columnName(message, field)).toList();
    List<String> referenced = options.getReferencesList().isEmpty() ? primaryKey(target)
        : options.getReferencesList().stream().map(field -> columnName(target, field)).toList();
    ForeignKey key = ForeignKey.of(table, columns, tableName(target), referenced, action(options.getOnDelete()));
    return named(key, options.getName()).withOnUpdate(action(options.getOnUpdate()));
  }

  private static Descriptor target(String owner, String name, Map<String, Descriptor> messages) {
    Descriptor target = messages.get(name);
    if (target == null) {
      throw new SchemaException("The foreign key of " + owner + " references " + name + ", which is not a table"
          + " message. Use the full message name of a message with the (leadseal.v1.table) option.");
    }
    return target;
  }

  private static List<String> primaryKey(Descriptor message) {
    return message.getFields().stream()
        .filter(field -> field.getOptions().getExtension(OptionsProto.column).getPrimaryKey())
        .map(field -> columnName(field, field.getOptions().getExtension(OptionsProto.column))).toList();
  }

  private static ForeignKey named(ForeignKey key, String name) {
    return name.isEmpty() ? key
        : new ForeignKey(name, key.columns(), key.table(), key.referencedColumns(), key.onDelete(), key.onUpdate());
  }

  private static ForeignKey.Action action(Action action) {
    return switch (action) {
      case ACTION_UNSPECIFIED, UNRECOGNIZED -> ForeignKey.Action.NO_ACTION;
      case ACTION_NO_ACTION -> ForeignKey.Action.NO_ACTION;
      case ACTION_RESTRICT -> ForeignKey.Action.RESTRICT;
      case ACTION_CASCADE -> ForeignKey.Action.CASCADE;
      case ACTION_SET_NULL -> ForeignKey.Action.SET_NULL;
    };
  }

  private static String columnName(Descriptor message, String fieldName) {
    FieldDescriptor field = message.findFieldByName(fieldName);
    if (field == null) {
      throw new SchemaException("The message " + message.getFullName() + " has no field '" + fieldName + "'.");
    }
    return columnName(field, field.getOptions().getExtension(OptionsProto.column));
  }

  private static String columnName(FieldDescriptor field, ColumnOptions options) {
    return options.getName().isEmpty() ? field.getName() : options.getName();
  }

  private static String tableName(Descriptor message) {
    String name = message.getOptions().getExtension(OptionsProto.table).getName();
    return name.isEmpty() ? snakeCase(message.getName()) : name;
  }

  private static String snakeCase(String name) {
    StringBuilder snake = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean boundary = i > 0 && Character.isUpperCase(c) && (Character.isLowerCase(name.charAt(i - 1))
          || i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)));
      snake.append(boundary ? "_" : "").append(c);
    }
    return snake.toString().toLowerCase(Locale.ROOT);
  }

  /** Returns the path of a message in its file descriptor, as source code info uses it. */
  private static List<Integer> path(Descriptor message) {
    if (message.getContainingType() == null) {
      return List.of(MESSAGE_TYPE, message.getIndex());
    }
    List<Integer> path = new ArrayList<>(path(message.getContainingType()));
    path.addAll(List.of(NESTED_TYPE, message.getIndex()));
    return path;
  }

  /** Returns the leading comment of an element of a file, or {@code null} if it has none. */
  private String comment(FileDescriptor file, List<Integer> path) {
    return comments.computeIfAbsent(file, ProtoSchema::comments).get(path);
  }

  private static Map<List<Integer>, String> comments(FileDescriptor file) {
    Map<List<Integer>, String> comments = new HashMap<>();
    for (SourceCodeInfo.Location location : file.toProto().getSourceCodeInfo().getLocationList()) {
      if (location.hasLeadingComments()) {
        String text = location.getLeadingComments().lines().map(String::strip).collect(Collectors.joining("\n"))
            .strip();
        if (!text.isEmpty()) {
          comments.put(List.copyOf(location.getPathList()), text);
        }
      }
    }
    return comments;
  }
}
