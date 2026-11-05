package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;

/** A default of a random UUID. */
public record RandomUuidDefault() implements GeneratedDefault {

  @Override
  public void validate(Column column) {
    if (!column.type().identifies()) {
      throw new InvalidSchemaException("The column '" + column.name() + "' defaults to a random UUID, but only UUID columns"
          + " can.");
    }
  }

  @Override
  public String sql(Dialect dialect, Column column) {
    return dialect.randomUuid();
  }
}
