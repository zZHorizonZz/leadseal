package dev.horizon.leadseal.protobuf.column;

import com.google.protobuf.Descriptors.EnumDescriptor;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
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
      throw refuse(value, "name");
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
      throw refuse(value, "number");
    }
    throw refuse(value, "number");
  }

  private SchemaException refuse(String value, String form) {
    return new InvalidSchemaException("The default '" + value + "' of the field " + field + " is not a value of "
        + values.getFullName() + ". Use a value " + form + ".");
  }
}
