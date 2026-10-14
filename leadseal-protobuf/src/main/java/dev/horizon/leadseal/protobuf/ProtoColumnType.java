package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.horizon.leadseal.BooleanType;
import dev.horizon.leadseal.BytesType;
import dev.horizon.leadseal.DateType;
import dev.horizon.leadseal.DecimalType;
import dev.horizon.leadseal.FloatType;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.JsonType;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.TextType;
import dev.horizon.leadseal.TimeType;
import dev.horizon.leadseal.TimestampType;
import dev.horizon.leadseal.Type;
import dev.horizon.leadseal.UuidType;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefaultAttribute;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * The column type of a field and the attributes that its type option sets: the default, the identity and the
 * collations.
 *
 * <p>
 * Without a type option, the type follows from the field type. A field can only have a type that holds all its values:
 * a {@code string} field can be text, a UUID, JSON, a decimal, a date, a time or a timestamp, a {@code bytes} field can
 * be bytes or a UUID, an enum field can be text, which stores the value name, or an integer, which stores the value
 * number, and 32-bit numbers can be 64-bit.
 *
 * @param field the field
 * @param options the column options of the field
 */
record ProtoColumnType(FieldDescriptor field, ColumnOptions options) {

  private static final Map<String, Type> MESSAGE_TYPES = Map.ofEntries(
      Map.entry("google.protobuf.Timestamp", new TimestampType()),
      Map.entry("google.protobuf.BoolValue", new BooleanType()),
      Map.entry("google.protobuf.Int32Value", new IntegerType(IntegerType.Size.INT32)),
      Map.entry("google.protobuf.UInt32Value", new IntegerType(IntegerType.Size.INT64)),
      Map.entry("google.protobuf.Int64Value", new IntegerType(IntegerType.Size.INT64)),
      Map.entry("google.protobuf.UInt64Value", new IntegerType(IntegerType.Size.INT64)),
      Map.entry("google.protobuf.FloatValue", new FloatType(FloatType.Size.FLOAT32)),
      Map.entry("google.protobuf.DoubleValue", new FloatType(FloatType.Size.FLOAT64)),
      Map.entry("google.protobuf.StringValue", new TextType(null)),
      Map.entry("google.protobuf.BytesValue", new BytesType(null)),
      Map.entry("google.type.Date", new DateType()),
      Map.entry("google.type.TimeOfDay", new TimeType()));
  private static final String DECIMAL_MESSAGE = "google.type.Decimal";

  /**
   * Returns the column type.
   *
   * @return the type
   * @throws SchemaException if the type option does not fit the field
   */
  Type type() {
    Type natural = natural();
    Type type = switch (options.getTypeCase()) {
      case TYPE_NOT_SET -> natural;
      case BOOLEAN -> new BooleanType();
      case INTEGER -> new IntegerType(switch (options.getInteger().getFormat()) {
        case FORMAT_INT32 -> IntegerType.Size.INT32;
        case FORMAT_INT64 -> IntegerType.Size.INT64;
        case FORMAT_UNSPECIFIED, UNRECOGNIZED -> natural instanceof IntegerType integer ? integer.size()
            : IntegerType.Size.INT32;
      });
      case NUMBER -> new FloatType(switch (options.getNumber().getFormat()) {
        case FORMAT_FLOAT -> FloatType.Size.FLOAT32;
        case FORMAT_DOUBLE -> FloatType.Size.FLOAT64;
        case FORMAT_UNSPECIFIED, UNRECOGNIZED -> natural instanceof FloatType number ? number.size()
            : FloatType.Size.FLOAT64;
      });
      case DECIMAL -> new DecimalType(options.getDecimal().getPrecision(), options.getDecimal().getScale());
      case TEXT -> new TextType(options.getText().getLength() == 0 ? null : options.getText().getLength());
      case BYTES -> new BytesType(options.getBytes().getLength() == 0 ? null : options.getBytes().getLength());
      case UUID -> new UuidType();
      case TIMESTAMP -> new TimestampType();
      case DATE -> new DateType();
      case TIME -> new TimeType();
      case JSON -> new JsonType();
    };
    if (natural == null) {
      throw new SchemaException("The field " + field.getFullName() + " is a " + DECIMAL_MESSAGE + ", so it needs a"
          + " decimal type option with a precision.");
    }
    if (!holds(natural, type)) {
      throw new SchemaException("The field " + field.getFullName() + " cannot have the column type " + type
          + ", because its values are " + natural + ".");
    }
    return type;
  }

  /**
   * Returns the attributes of the type option: the default, the identity and the collations.
   *
   * @return the attributes
   * @throws SchemaException if a default is invalid
   */
  List<ColumnAttribute> attributes() {
    List<ColumnAttribute> attributes = new ArrayList<>();
    switch (options.getTypeCase()) {
      case BOOLEAN -> {
        if (options.getBoolean().hasDefaultValue()) {
          attributes.add(literal(String.valueOf(options.getBoolean().getDefaultValue())));
        }
      }
      case INTEGER -> {
        if (options.getInteger().hasDefaultValue()) {
          attributes.add(literal(String.valueOf(options.getInteger().getDefaultValue())));
        }
        if (options.getInteger().getIdentity()) {
          attributes.add(IdentityAttribute.INSTANCE);
        }
      }
      case NUMBER -> {
        if (options.getNumber().hasDefaultValue()) {
          attributes.add(literal(BigDecimal.valueOf(options.getNumber().getDefaultValue()).toPlainString()));
        }
      }
      case DECIMAL -> {
        if (options.getDecimal().hasDefaultValue()) {
          attributes.add(literal(options.getDecimal().getDefaultValue()));
        }
      }
      case TEXT -> {
        if (options.getText().hasDefaultValue()) {
          attributes.add(literal(options.getText().getDefaultValue()));
        }
        options.getText().getCollationMap()
            .forEach((dialect, collation) -> attributes.add(new CollationAttribute(dialect, collation)));
      }
      case BYTES -> {
        if (options.getBytes().hasDefaultValue()) {
          attributes.add(literal(HexFormat.of().formatHex(options.getBytes().getDefaultValue().toByteArray())));
        }
      }
      case UUID -> {
        one(options.getUuid().hasDefaultValue(), options.getUuid().getDefaultRandom());
        if (options.getUuid().hasDefaultValue()) {
          attributes.add(literal(options.getUuid().getDefaultValue()));
        } else if (options.getUuid().getDefaultRandom()) {
          attributes.add(new RandomUuidDefaultAttribute());
        }
      }
      case TIMESTAMP -> {
        one(options.getTimestamp().hasDefaultValue(), options.getTimestamp().getDefaultNow());
        if (options.getTimestamp().hasDefaultValue()) {
          attributes.add(literal(Instant.ofEpochSecond(options.getTimestamp().getDefaultValue().getSeconds(),
              options.getTimestamp().getDefaultValue().getNanos()).toString()));
        } else if (options.getTimestamp().getDefaultNow()) {
          attributes.add(new NowDefaultAttribute());
        }
      }
      case DATE -> {
        one(options.getDate().hasDefaultValue(), options.getDate().getDefaultToday());
        if (options.getDate().hasDefaultValue()) {
          attributes.add(literal(options.getDate().getDefaultValue()));
        } else if (options.getDate().getDefaultToday()) {
          attributes.add(new NowDefaultAttribute());
        }
      }
      case TIME -> {
        if (options.getTime().hasDefaultValue()) {
          attributes.add(literal(options.getTime().getDefaultValue()));
        }
      }
      case JSON -> {
        if (options.getJson().hasDefaultValue()) {
          attributes.add(literal(options.getJson().getDefaultValue()));
        }
      }
      case TYPE_NOT_SET -> {
      }
    }
    return attributes;
  }

  /** Returns the type that follows from the field type, or {@code null} for a decimal message. */
  private Type natural() {
    if (field.isRepeated()) {
      return new JsonType();
    }
    return switch (field.getJavaType()) {
      case BOOLEAN -> new BooleanType();
      case INT -> new IntegerType(field.getType() == FieldDescriptor.Type.UINT32
          || field.getType() == FieldDescriptor.Type.FIXED32 ? IntegerType.Size.INT64 : IntegerType.Size.INT32);
      case LONG -> new IntegerType(IntegerType.Size.INT64);
      case FLOAT -> new FloatType(FloatType.Size.FLOAT32);
      case DOUBLE -> new FloatType(FloatType.Size.FLOAT64);
      case STRING, ENUM -> new TextType(null);
      case BYTE_STRING -> new BytesType(null);
      case MESSAGE -> DECIMAL_MESSAGE.equals(field.getMessageType().getFullName())
          ? options.getTypeCase() == ColumnOptions.TypeCase.DECIMAL ? type(options) : null
          : MESSAGE_TYPES.getOrDefault(field.getMessageType().getFullName(), new JsonType());
    };
  }

  private static Type type(ColumnOptions options) {
    return new DecimalType(options.getDecimal().getPrecision(), options.getDecimal().getScale());
  }

  /** Returns whether a column of a type holds every value of a field whose own type is {@code natural}. */
  private boolean holds(Type natural, Type type) {
    boolean enumField = field.getJavaType() == FieldDescriptor.JavaType.ENUM && !field.isRepeated();
    return switch (type) {
      case TextType _ -> natural instanceof TextType;
      case IntegerType integer -> enumField || natural instanceof IntegerType own
          && (own.size() == integer.size() || integer.size() == IntegerType.Size.INT64);
      case FloatType number -> natural instanceof FloatType own
          && (own.size() == number.size() || number.size() == FloatType.Size.FLOAT64);
      case UuidType _ -> !enumField && (natural instanceof TextType || natural instanceof BytesType);
      case JsonType _,DecimalType _,DateType _,TimeType _,TimestampType _ -> natural.getClass() == type.getClass()
          || !enumField && natural instanceof TextType;
      case BooleanType _,BytesType _ -> natural.getClass() == type.getClass();
    };
  }

  private LiteralDefaultAttribute literal(String value) {
    boolean enumField = field.getJavaType() == FieldDescriptor.JavaType.ENUM && !field.isRepeated();
    if (enumField) {
      new ProtoEnumValue(field.getEnumType(), value).check(options.getTypeCase() == ColumnOptions.TypeCase.TEXT,
          field.getFullName());
    }
    return new LiteralDefaultAttribute(value);
  }

  private void one(boolean fixed, boolean generated) {
    if (fixed && generated) {
      throw new SchemaException("The field " + field.getFullName() + " has both a default_value and a generated"
          + " default. Set one of them.");
    }
  }
}
