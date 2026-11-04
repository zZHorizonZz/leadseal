package dev.horizon.leadseal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A migration from one schema to another: the origin schema before it runs and the target schema after it runs. A
 * change reads the tables and columns that it changes from a transition, so the SQL of the change has their full
 * definitions.
 *
 * @param origin the schema before the migration
 * @param target the schema after the migration
 */
public record Transition(Schema origin, Schema target) {

  public Transition {
    Objects.requireNonNull(origin, "origin");
    Objects.requireNonNull(target, "target");
  }

  /**
   * Returns the name that a table has in the target schema, found by its id.
   *
   * @param table the name of the table in the origin schema
   * @return the name in the target schema, or the same name if the target schema has no table with its id
   */
  public TableName targetName(TableName table) {
    return origin.table(table).flatMap(source -> target.nameOf(source.id())).orElse(table);
  }

  /**
   * Returns the name that a column has in the target schema, found by the ids of its table and of the column.
   *
   * @param table the name of the table in the origin schema
   * @param column the name of the column in the origin schema
   * @return the name in the target schema, or the same name if the target schema has no such column
   */
  public String targetColumn(TableName table, String column) {
    return origin.table(table).flatMap(source -> source.column(column).map(Column::id)
        .flatMap(id -> target.tableById(source.id()).flatMap(next -> next.columnById(id)))).map(Column::name)
        .orElse(column);
  }

  /**
   * Returns the names that columns have in the target schema.
   *
   * @param table the name of the table in the origin schema
   * @param columns the names of the columns in the origin schema
   * @return the names in the target schema, in the same order
   * @see #targetColumn(TableName, String)
   */
  public List<String> targetColumns(TableName table, List<String> columns) {
    return columns.stream().map(column -> targetColumn(table, column)).toList();
  }

  /**
   * Gives a table of the migration by its name after the migration, or before it if the migration drops it.
   *
   * @param name the name of the table
   * @return the table
   * @throws SchemaException if neither schema has a table of the name
   */
  public TableTransition table(TableName name) {
    return new TableTransition(this, name, target.table(name).or(() -> origin.table(name)).map(Table::id)
        .orElseThrow(() -> new InvalidSchemaException("The table " + name + " is in neither schema of the migration.")));
  }

  /**
   * Gives a table of the migration by its name before the migration, or after it if the migration creates it.
   *
   * @param name the name of the table
   * @return the table
   * @throws SchemaException if neither schema has a table of the name
   */
  public TableTransition originTable(TableName name) {
    return new TableTransition(this, name, origin.table(name).or(() -> target.table(name)).map(Table::id)
        .orElseThrow(() -> new InvalidSchemaException("The table " + name + " is in neither schema of the migration.")));
  }

  /**
   * A table of a migration: the same table, by its id, in both schemas.
   *
   * @param transition the migration
   * @param name the name of the table in the statement that changes it
   * @param id the id of the table
   */
  public record TableTransition(Transition transition, TableName name, String id) {

    public TableTransition {
      Objects.requireNonNull(transition, "transition");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(id, "id");
    }

    /**
     * Gives the table before the migration.
     *
     * @return the table, or empty if the migration creates it
     */
    public Optional<Table> origin() {
      return transition.origin.tableById(id);
    }

    /**
     * Gives the table after the migration.
     *
     * @return the table, or empty if the migration drops it
     */
    public Optional<Table> target() {
      return transition.target.tableById(id);
    }

    /**
     * Gives the table after the migration, which a change of the table needs.
     *
     * @return the table
     * @throws SchemaException if the migration drops the table
     */
    public Table definition() {
      return target().orElseThrow(() -> new InvalidSchemaException("The table " + name + " is not in the schema after the"
          + " migration."));
    }

    /**
     * Gives a column of the table.
     *
     * @param name the name of the column in the target table, or in the origin table if the migration drops it
     * @return the column
     */
    public ColumnTransition column(String name) {
      return new ColumnTransition(this, name);
    }
  }

  /**
   * A column of a migration. It finds the same column in the other schema by its id.
   *
   * @param table the table of the column
   * @param name the name of the column in the target table, or in the origin table if the migration drops it
   */
  public record ColumnTransition(TableTransition table, String name) {

    public ColumnTransition {
      Objects.requireNonNull(table, "table");
      Objects.requireNonNull(name, "name");
    }

    /**
     * Gives the column before the migration.
     *
     * @return the column, or empty if the migration adds it
     */
    public Optional<Column> origin() {
      return table.origin().flatMap(origin -> table.target().flatMap(target -> target.column(name))
          .flatMap(column -> origin.columnById(column.id())).or(() -> origin.column(name)));
    }

    /**
     * Gives the column after the migration, which a change of the column needs.
     *
     * @return the column
     * @throws SchemaException if the migration drops the column
     */
    public Column definition() {
      return table.definition().column(name).orElseThrow(() -> new InvalidSchemaException("The column " + table.name()
          + "." + name + " is not in the schema after the migration."));
    }
  }
}
