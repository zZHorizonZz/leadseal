package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;
import java.util.Optional;

/**
 * The primary key of a table. Every table has one.
 *
 * @param name the constraint name
 * @param columns the key columns, in order
 */
public record PrimaryKey(String name, List<String> columns) implements TableAttribute {

  private static final String PREFIX = "pk";

  public PrimaryKey {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("A primary key has no name.");
    }
    columns = List.copyOf(columns);
    if (columns.isEmpty()) {
      throw new InvalidSchemaException("The primary key '" + name + "' has no columns.");
    }
  }

  /**
   * Returns a primary key with a generated name: {@code pk_} followed by the table name.
   *
   * @param table the table name
   * @param columns the key columns
   * @return the primary key
   */
  public static PrimaryKey of(String table, List<String> columns) {
    return new PrimaryKey(new ConstraintName(PREFIX, table, List.of()).value(), columns);
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
      Column column = table.column(key).orElseThrow(() -> new InvalidSchemaException("The primary key '" + name + "'"
          + " refers to the column '" + key + "', which the table '" + table.name() + "' does not have."));
      if (column.nullable()) {
        throw new InvalidSchemaException("The primary key column '" + table.name() + "." + key + "' must not accept NULL.");
      }
    }
  }

  @Override
  public Optional<TableAttribute> rename(TableName table, Transition transition) {
    return transition.targetName(table).flatMap(target -> transition.targetColumns(table, columns).map(
        key -> new PrimaryKey(name.equals(new ConstraintName(PREFIX, table.name(), List.of()).value())
            ? new ConstraintName(PREFIX, target.name(), List.of()).value()
            : name, key)));
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ALTER_PRIMARY_KEYS;
  }

  @Override
  public boolean alterable() {
    return true;
  }

  @Override
  public List<Command> add(Ddl ddl, TableTransition table) {
    throw new IllegalStateException("A table has its primary key from its creation, so the key cannot be added.");
  }

  @Override
  public List<Command> drop(Ddl ddl, TableTransition table) {
    throw new IllegalStateException("A table keeps its primary key until it is dropped, so the key cannot be dropped.");
  }

  @Override
  public List<Command> alter(Ddl ddl, TableTransition table) {
    PrimaryKey origin = table.origin().orElseThrow(() -> new IllegalStateException("The table " + table.name()
        + " has no primary key to change, because the migration creates it.")).primaryKey();
    String keyName = table.originName().name().equals(table.name().name()) ? origin.name() : name;
    return ddl.constraint().alterPrimaryKey(table, new PrimaryKey(keyName, origin.columns()), this);
  }

  @Override
  public boolean covers(String column) {
    return columns.contains(column);
  }
}
