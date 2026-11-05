package dev.horizon.leadseal.protobuf.column;

import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import java.util.Map;

public sealed interface FieldKind permits ValueField, EnumField, DecimalField {

  Map<String, Type> MESSAGE_TYPES = Map.ofEntries(
      Map.entry("google.protobuf.Timestamp", new TimestampType()),
      Map.entry("google.protobuf.BoolValue", new BooleanType()),
      Map.entry("google.protobuf.Int32Value", new IntegerType(IntegerType.Size.INT32)),
      Map.entry("google.protobuf.UInt32Value", new IntegerType(IntegerType.Size.INT64)),
      Map.entry("google.protobuf.Int64Value", new IntegerType(IntegerType.Size.INT64)),
      Map.entry("google.protobuf.UInt64Value", new IntegerType(IntegerType.Size.INT64)),
      Map.entry("google.protobuf.FloatValue", new FloatType(FloatType.Size.FLOAT32)),
      Map.entry("google.protobuf.DoubleValue", new FloatType(FloatType.Size.FLOAT64)),
      Map.entry("google.protobuf.StringValue", TextType.unlimited()),
      Map.entry("google.protobuf.BytesValue", BytesType.unlimited()),
      Map.entry("google.type.Date", new DateType()),
      Map.entry("google.type.TimeOfDay", new TimeType()));

  static FieldKind of(FieldDescriptor field) {
    if (field.isRepeated()) {
      return new ValueField(field.getFullName(), new JsonType());
    }
    return switch (field.getJavaType()) {
      case BOOLEAN -> new ValueField(field.getFullName(), new BooleanType());
      case INT -> new ValueField(field.getFullName(), new IntegerType(field.getType() == FieldDescriptor.Type.UINT32
          || field.getType() == FieldDescriptor.Type.FIXED32 ? IntegerType.Size.INT64 : IntegerType.Size.INT32));
      case LONG -> new ValueField(field.getFullName(), new IntegerType(IntegerType.Size.INT64));
      case FLOAT -> new ValueField(field.getFullName(), new FloatType(FloatType.Size.FLOAT32));
      case DOUBLE -> new ValueField(field.getFullName(), new FloatType(FloatType.Size.FLOAT64));
      case STRING -> new ValueField(field.getFullName(), TextType.unlimited());
      case ENUM -> new EnumField(field.getFullName(), field.getEnumType());
      case BYTE_STRING -> new ValueField(field.getFullName(), BytesType.unlimited());
      case MESSAGE -> DecimalField.MESSAGE.equals(field.getMessageType().getFullName())
          ? new DecimalField(field.getFullName())
          : new ValueField(field.getFullName(), MESSAGE_TYPES.getOrDefault(field.getMessageType().getFullName(),
              new JsonType()));
    };
  }

  String field();

  Type natural();

  boolean holds(Type type);

  default LiteralDefault literal(String value) {
    return new LiteralDefault(value);
  }

  default LiteralDefault text(String value) {
    return literal(value);
  }

  default Type columnType(Type type) {
    return type;
  }
}
