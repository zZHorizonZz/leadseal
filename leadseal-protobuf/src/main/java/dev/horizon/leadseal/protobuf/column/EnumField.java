package dev.horizon.leadseal.protobuf.column;

import com.google.protobuf.Descriptors.EnumDescriptor;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;

record EnumField(String field, EnumDescriptor values) implements FieldKind {

  @Override
  public Type natural() {
    return TextType.unlimited();
  }

  @Override
  public boolean holds(Type type) {
    return type instanceof TextType || type instanceof IntegerType;
  }

  @Override
  public LiteralDefault text(String value) {
    if (values.findValueByName(value) == null) {
      throw new UnknownEnumValueException(field, values, value, "name");
    }
    return new LiteralDefault(value);
  }

  @Override
  public LiteralDefault literal(String value) {
    try {
      if (values.findValueByNumber(Integer.parseInt(value)) != null) {
        return new LiteralDefault(value);
      }
    } catch (NumberFormatException _) {
      throw new UnknownEnumValueException(field, values, value, "number");
    }
    throw new UnknownEnumValueException(field, values, value, "number");
  }
}
