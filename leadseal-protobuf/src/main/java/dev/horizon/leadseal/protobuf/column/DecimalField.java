package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.Type;

record DecimalField(String field) implements FieldKind {

  static final String MESSAGE = "google.type.Decimal";

  @Override
  public Type natural() {
    throw new InvalidSchemaException("The field " + field + " is a " + MESSAGE + ", so it needs a decimal type option with"
        + " a precision.");
  }

  @Override
  public boolean holds(Type type) {
    return type instanceof DecimalType;
  }
}
