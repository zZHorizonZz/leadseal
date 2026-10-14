package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.TextType;
import java.util.List;

/**
 * The collation of a text column in one database. A column has at most one collation for each dialect, and a dialect
 * without one uses the database default.
 *
 * @param dialect the dialect name, for example {@code postgresql}
 * @param name the collation name in that database, for example {@code C}
 */
public record CollationAttribute(String dialect, String name) implements ColumnAttribute {

  public CollationAttribute {
    if (dialect == null || dialect.isBlank() || name == null || name.isBlank()) {
      throw new SchemaException("A collation needs a dialect and a name.");
    }
  }

  @Override
  public Object key() {
    return List.of(CollationAttribute.class, dialect);
  }

  @Override
  public void validate(Column column) {
    if (!(column.type() instanceof TextType)) {
      throw new SchemaException("The column '" + column.name() + "' has a collation, but only text columns can have"
          + " one.");
    }
  }
}
