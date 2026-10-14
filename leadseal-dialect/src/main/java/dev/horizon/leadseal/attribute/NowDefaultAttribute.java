package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DateType;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.TimestampType;

/** A default of the current time, or the current date for a date column, in UTC. */
public record NowDefaultAttribute() implements DefaultAttribute {

  @Override
  public void validate(Column column) {
    if (!(column.type() instanceof TimestampType || column.type() instanceof DateType)) {
      throw new SchemaException("The column '" + column.name() + "' defaults to the current time, but only timestamp"
          + " and date columns can.");
    }
  }
}
