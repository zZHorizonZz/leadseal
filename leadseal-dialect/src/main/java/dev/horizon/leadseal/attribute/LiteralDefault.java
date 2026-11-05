package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;

/**
 * A fixed default value.
 *
 * @param value the value, in the canonical text form of the column type, for example {@code 0} or {@code new}
 */
public record LiteralDefault(@JsonInclude(JsonInclude.Include.ALWAYS) String value)
    implements
      Default {

  public LiteralDefault {
    if (value == null) {
      throw new InvalidSchemaException("A literal default has no value.");
    }
  }

  @Override
  public void validate(Column column) {
    if (!column.type().accepts(value)) {
      throw new InvalidSchemaException("The default '" + value + "' of the column '" + column.name() + "' is not a value of"
          + " its type " + column.type() + ". An example of a value is " + column.type().example() + ".");
    }
  }

  @Override
  public boolean constant() {
    return true;
  }

  @Override
  public String sql(Dialect dialect, Column column) {
    return dialect.literal(column.type(), value);
  }
}
