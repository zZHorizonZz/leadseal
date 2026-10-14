package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;

/**
 * A fixed default value.
 *
 * @param value the value, in the canonical text form of the column type, for example {@code 0} or {@code new}
 */
public record LiteralDefaultAttribute(@JsonInclude(JsonInclude.Include.ALWAYS) String value)
    implements
      DefaultAttribute {

  public LiteralDefaultAttribute {
    if (value == null) {
      throw new SchemaException("A literal default has no value.");
    }
  }

  @Override
  public void validate(Column column) {
    if (!column.type().accepts(value)) {
      throw new SchemaException("The default '" + value + "' of the column '" + column.name() + "' is not a value of"
          + " its type " + column.type() + ". An example of a value is " + column.type().example() + ".");
    }
  }
}
