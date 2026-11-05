package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The collation of a text column in one database. A column has at most one collation for each dialect, and a dialect
 * without one uses the database default.
 *
 * @param dialect the dialect name, for example {@code postgresql}
 * @param name the collation name in that database, for example {@code C}
 */
public record Collation(String dialect, String name) implements ColumnAttribute {

  private static final Pattern BARE = Pattern.compile("[A-Za-z0-9_]+");

  public Collation {
    if (dialect == null || dialect.isBlank() || name == null || name.isBlank()) {
      throw new InvalidSchemaException("A collation needs a dialect and a name.");
    }
  }

  @Override
  public Object key() {
    return List.of(Collation.class, dialect);
  }

  @Override
  public void validate(Column column) {
    if (!column.type().collates()) {
      throw new InvalidSchemaException("The column '" + column.name() + "' has a collation, but only text columns can have"
          + " one.");
    }
  }

  @Override
  public List<Command> add(Ddl ddl, ColumnTransition column) {
    return ddl.column().collate(column, this);
  }

  @Override
  public List<Command> drop(Ddl ddl, ColumnTransition column) {
    return ddl.column().collate(column, this);
  }

  /**
   * Tells if this collation is a collation of a dialect. A column can have one collation for each dialect.
   *
   * @param dialect the dialect
   * @return true if the collation is for the dialect
   */
  public boolean appliesTo(Dialect dialect) {
    return this.dialect.equals(dialect.name());
  }

  /**
   * Gives the name of this collation for a database that writes a collation without quotes.
   *
   * @return the name
   * @throws SchemaException if the name has other characters than letters, digits and underscores
   */
  public String bareName() {
    if (!BARE.matcher(name).matches()) {
      throw new InvalidSchemaException("The " + dialect + " collation '" + name + "' is not a collation name.");
    }
    return name;
  }
}
