package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.SchemaException;

/**
 * A column whose values the database generates for new rows. An identity column is an integer, does not accept
 * {@code NULL}, has no default, and is the only column of the primary key of its table.
 */
public record IdentityAttribute() implements ColumnAttribute {

  public static final IdentityAttribute INSTANCE = new IdentityAttribute();

  @Override
  public void validate(Column column) {
    if (!(column.type() instanceof IntegerType) || column.nullable()
        || column.attribute(DefaultAttribute.class).isPresent()) {
      throw new SchemaException("The identity column '" + column.name() + "' must be an integer column that does not"
          + " accept NULL and has no default.");
    }
  }
}
