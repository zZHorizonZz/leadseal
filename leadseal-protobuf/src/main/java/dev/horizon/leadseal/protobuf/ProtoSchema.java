package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.PrimaryKey;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads the table messages of protobuf files into a schema.
 *
 * <p>
 * A message is a table if it has the {@code (leadseal.v1.table)} option. Each field is a column, unless its
 * {@code (leadseal.v1.column)} option has {@code ignore} set. The full message name is the table id and the field
 * number is the column id, so renaming a message or a field renames its table or column and keeps the rows.
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
 * <li>{@code google.protobuf.Timestamp}: {@code TIMESTAMP}.</li>
 * <li>Wrapper messages such as {@code google.protobuf.Int64Value}: the type of the wrapped value.</li>
 * <li>Other messages, repeated fields and maps: {@code JSON}.</li>
 * </ul>
 *
 * <p>
 * A column accepts {@code NULL} if its field has presence, for example a field with the {@code optional} label or a
 * message field, and for repeated fields and maps.
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
      Map.entry("google.protobuf.BytesValue", ColumnType.BYTES));
  private static final Map<ColumnType, Set<ColumnType>> OVERRIDES = Map.of(
      ColumnType.TEXT, Set.of(ColumnType.UUID, ColumnType.JSON),
      ColumnType.BYTES, Set.of(ColumnType.UUID),
      ColumnType.INT, Set.of(ColumnType.BIGINT));

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

  private static Table table(Descriptor message, Map<String, Descriptor> messages) {
    String name = tableName(message);
    List<Column> columns = new ArrayList<>();
    List<String> primaryKey = new ArrayList<>();
    List<Index> indexes = new ArrayList<>();
    List<ForeignKey> foreignKeys = new ArrayList<>();
    for (FieldDescriptor field : message.getFields()) {
      ColumnOptions options = field.getOptions().getExtension(OptionsProto.column);
      if (options.getIgnore()) {
        continue;
      }
      Column column = column(field, options);
      columns.add(column);
      if (options.getPrimaryKey()) {
        primaryKey.add(column.name());
      }
      if (options.getUnique() || options.getIndex()) {
        indexes.add(Index.of(name, List.of(column.name()), options.getUnique()));
      }
      if (options.hasForeignKey()) {
        foreignKeys.add(foreignKey(name, field, column, options.getForeignKey(), messages));
      }
    }
    for (IndexOptions index : message.getOptions().getExtension(OptionsProto.table).getIndexList()) {
      List<String> indexed = index.getFieldsList().stream().map(field -> columnName(message, field)).toList();
      indexes.add(index.getName().isEmpty() ? Index.of(name, indexed, index.getUnique())
          : new Index(index.getName(), indexed, index.getUnique()));
    }
    if (primaryKey.isEmpty()) {
      throw new SchemaException("The table message " + message.getFullName() + " has no primary key. Set"
          + " (leadseal.v1.column).primary_key on its key fields.");
    }
    return new Table(message.getFullName(), name, columns, PrimaryKey.of(name, primaryKey), indexes, foreignKeys);
  }

  private static Column column(FieldDescriptor field, ColumnOptions options) {
    ColumnType type = defaultType(field);
    if (options.getType() != ColumnOptions.Type.TYPE_UNSPECIFIED) {
      ColumnType wanted = ColumnType.valueOf(options.getType().name().substring("TYPE_".length()));
      if (wanted != type && !OVERRIDES.getOrDefault(type, Set.of()).contains(wanted)) {
        throw new SchemaException("The field " + field.getFullName() + " cannot have the column type " + wanted
            + ", because its values are " + type + ".");
      }
      type = wanted;
    }
    Integer length = options.getLength() == 0 ? null : options.getLength();
    boolean nullable = field.isRepeated() || field.hasPresence() && !field.isRequired();
    return new Column(String.valueOf(field.getNumber()), columnName(field, options), type, length, nullable);
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
    Descriptor target = messages.get(options.getMessage());
    if (target == null) {
      throw new SchemaException("The foreign key of " + field.getFullName() + " references " + options.getMessage()
          + ", which is not a table message. Use the full message name of a message with the (leadseal.v1.table)"
          + " option.");
    }
    String referenced;
    if (options.getField().isEmpty()) {
      List<FieldDescriptor> keys = target.getFields().stream()
          .filter(key -> key.getOptions().getExtension(OptionsProto.column).getPrimaryKey()).toList();
      if (keys.size() != 1) {
        throw new SchemaException("The foreign key of " + field.getFullName() + " needs a field, because "
            + target.getFullName() + " has " + keys.size() + " primary key fields.");
      }
      referenced = columnName(keys.getFirst(), keys.getFirst().getOptions().getExtension(OptionsProto.column));
    } else {
      referenced = columnName(target, options.getField());
    }
    ForeignKey.Action action = options.getOnDelete() == ForeignKeyOptions.Action.ACTION_UNSPECIFIED
        ? ForeignKey.Action.NO_ACTION
        : ForeignKey.Action.valueOf(options.getOnDelete().name().substring("ACTION_".length()));
    return ForeignKey.of(table, List.of(column.name()), tableName(target), List.of(referenced), action);
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
}
