package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;

/** A default of the current time, or the current date for a date column, in UTC. */
public record NowDefault() implements GeneratedDefault {

  @Override
  public void validate(Column column) {
    if (!column.type().dates()) {
      throw new InvalidSchemaException("The column '" + column.name() + "' defaults to the current time, but only timestamp"
          + " and date columns can.");
    }
  }

  @Override
  public String sql(Dialect dialect, Column column) {
    return column.type().current(dialect);
  }
}
