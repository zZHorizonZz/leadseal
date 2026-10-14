package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Renaming;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.change.Change;
import java.util.List;

/**
 * The primary key of a table. Every table has one.
 *
 * @param name the constraint name
 * @param columns the key columns, in order
 */
public record PrimaryKeyAttribute(String name, List<String> columns) implements TableAttribute {

  private static final String PREFIX = "pk";

  public PrimaryKeyAttribute {
    if (name == null || name.isBlank()) {
      throw new SchemaException("A primary key has no name.");
    }
    columns = List.copyOf(columns);
    if (columns.isEmpty()) {
      throw new SchemaException("The primary key '" + name + "' has no columns.");
    }
  }

  /**
   * Returns a primary key with a generated name: {@code pk_} followed by the table name.
   *
   * @param table the table name
   * @param columns the key columns
   * @return the primary key
   */
  public static PrimaryKeyAttribute of(String table, List<String> columns) {
    return new PrimaryKeyAttribute(new ConstraintName(PREFIX, table, List.of()).value(), columns);
  }

  @Override
  public String summary(String table) {
    return table + "_primary_key";
  }

  @Override
  public boolean minor() {
    return false;
  }

  @Override
  public void validate(Table table) {
    for (String key : columns) {
      Column column = table.column(key).orElseThrow(() -> new SchemaException("The primary key '" + name + "'"
          + " refers to the column '" + key + "', which the table '" + table.name() + "' does not have."));
      if (column.nullable()) {
        throw new SchemaException("The primary key column '" + table.name() + "." + key + "' must not accept NULL.");
      }
    }
  }

  @Override
  public TableAttribute renamed(TableName table, Renaming renaming) {
    String newTable = renaming.table(table).name();
    String renamedName = name.equals(new ConstraintName(PREFIX, table.name(), List.of()).value())
        ? new ConstraintName(PREFIX, newTable, List.of()).value()
        : name;
    return new PrimaryKeyAttribute(renamedName, renaming.columns(table, columns));
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ALTER_PRIMARY_KEYS;
  }

  @Override
  public boolean alterable() {
    return true;
  }
}
