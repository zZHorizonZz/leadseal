package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.UuidType;

/** A default of a random UUID. */
public record RandomUuidDefaultAttribute() implements DefaultAttribute {

  @Override
  public void validate(Column column) {
    if (!(column.type() instanceof UuidType)) {
      throw new SchemaException("The column '" + column.name() + "' defaults to a random UUID, but only UUID columns"
          + " can.");
    }
  }
}
