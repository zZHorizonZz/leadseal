package dev.horizon.leadseal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A migration from one schema to another: the origin schema before it runs and the target schema after it runs. A
 * change reads the tables and columns that it changes from a transition, so the SQL of the change has their full
 * definitions. A table and a column are the same in both schemas when they have the same id.
 */
public final class Transition {

  private final Schema origin;
  private final Schema target;
  private final Map<String, TableName> originNames;
  private final Map<String, TableName> targetNames;

  /**
   * Creates the migration from one schema to another.
   *
   * @param origin the schema before the migration
   * @param target the schema after the migration
   */
  public Transition(Schema origin, Schema target) {
    this.origin = Objects.requireNonNull(origin, "origin");
    this.target = Objects.requireNonNull(target, "target");
    this.originNames = names(origin);
    this.targetNames = names(target);
  }

  /**
   * Returns the schema before the migration.
   *
   * @return the schema
   */
  public Schema origin() {
    return origin;
  }

  /**
   * Returns the schema after the migration.
   *
   * @return the schema
   */
  public Schema target() {
    return target;
  }

  /**
   * Returns the name that a table of the origin schema has in the target schema, found by its id.
   *
   * @param table the name of the table in the origin schema
   * @return the name in the target schema, or empty if the target schema has no table with its id
   * @throws IllegalArgumentException if the origin schema has no table of the name
   */
  public Optional<TableName> targetName(TableName table) {
    return Optional.ofNullable(targetNames.get(originDefinition(table).id()));
  }

  /**
   * Returns the name that a column of the origin schema has in the target schema, found by the ids of its table and
   * of the column.
   *
   * @param table the name of the table in the origin schema
   * @param column the name of the column in the origin schema
   * @return the name in the target schema, or empty if the target schema has no such column
   * @throws IllegalArgumentException if the origin schema has no such column
   */
  public Optional<String> targetColumn(TableName table, String column) {
    Table definition = originDefinition(table);
    String id = definition.column(column).orElseThrow(() -> new IllegalArgumentException("The origin table " + table
        + " has no column " + column + ".")).id();
    return tableById(target, targetNames, definition.id()).flatMap(targetTable -> targetTable.columnById(id)).map(Column::name);
  }

  /**
   * Returns the names that columns of the origin schema have in the target schema.
   *
   * @param table the name of the table in the origin schema
   * @param columns the names of the columns in the origin schema
   * @return the names in the target schema, in the same order, or empty if the target schema lacks one of the columns
   * @throws IllegalArgumentException if the origin schema has no such column
   * @see #targetColumn(TableName, String)
   */
  public Optional<List<String>> targetColumns(TableName table, List<String> columns) {
    List<Optional<String>> names = columns.stream().map(column -> targetColumn(table, column)).toList();
    return names.stream().allMatch(Optional::isPresent)
        ? Optional.of(names.stream().map(Optional::orElseThrow).toList())
        : Optional.empty();
  }

  /**
   * Gives a table of the migration by its name in the target schema.
   *
   * @param name the name of the table in the target schema
   * @return the table
   * @throws InvalidSchemaException if the target schema has no table of the name
   */
  public TableTransition table(TableName name) {
    return new TableTransition(this, name, target.table(name).map(Table::id).orElseThrow(
        () -> new InvalidSchemaException("The table " + name + " is not in the schema after the migration.")));
  }

  /**
   * Gives a table of the migration by its name in the origin schema, for a change that runs before the renames.
   *
   * @param name the name of the table in the origin schema
   * @return the table
   * @throws InvalidSchemaException if the origin schema has no table of the name
   */
  public TableTransition originTable(TableName name) {
    return new TableTransition(this, name, origin.table(name).map(Table::id).orElseThrow(
        () -> new InvalidSchemaException("The table " + name + " is not in the schema before the migration.")));
  }

  private Table originDefinition(TableName name) {
    return origin.table(name).orElseThrow(() -> new IllegalArgumentException("The origin schema has no table " + name
        + "."));
  }

  private static Map<String, TableName> names(Schema schema) {
    Map<String, TableName> names = new HashMap<>();
    schema.names().forEach(name -> names.put(schema.table(name).orElseThrow().id(), name));
    return names;
  }

  private static Optional<Table> tableById(Schema schema, Map<String, TableName> names, String id) {
    return Optional.ofNullable(names.get(id)).flatMap(schema::table);
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
      return tableById(transition.origin, transition.originNames, id);
    }

    /**
     * Gives the table after the migration.
     *
     * @return the table, or empty if the migration drops it
     */
    public Optional<Table> target() {
      return tableById(transition.target, transition.targetNames, id);
    }

    /**
     * Gives the name of the table before the migration.
     *
     * @return the name
     * @throws InvalidSchemaException if the migration creates the table
     */
    public TableName originName() {
      return Optional.ofNullable(transition.originNames.get(id)).orElseThrow(() -> new InvalidSchemaException("The table "
          + name + " is not in the schema before the migration."));
    }

    /**
     * Gives the table after the migration, which a change of the table needs.
     *
     * @return the table
     * @throws InvalidSchemaException if the migration drops the table
     */
    public Table definition() {
      return target().orElseThrow(() -> new InvalidSchemaException("The table " + name + " is not in the schema after the"
          + " migration."));
    }

    /**
     * Gives a column of the table by its name in the target table.
     *
     * @param name the name of the column in the target table
     * @return the column
     * @throws InvalidSchemaException if the target table has no column of the name
     */
    public ColumnTransition column(String name) {
      return new ColumnTransition(this, name, definition().column(name).map(Column::id).orElseThrow(
          () -> new InvalidSchemaException("The column " + this.name + "." + name + " is not in the schema after the"
              + " migration.")));
    }

    /**
     * Gives a column of the table by its name in the origin table, for a column that the migration drops.
     *
     * @param name the name of the column in the origin table
     * @return the column
     * @throws InvalidSchemaException if the origin table has no column of the name
     */
    public ColumnTransition originColumn(String name) {
      return new ColumnTransition(this, name, origin().flatMap(table -> table.column(name)).map(Column::id)
          .orElseThrow(() -> new InvalidSchemaException("The column " + this.name + "." + name + " is not in the schema"
              + " before the migration.")));
    }
  }

  /**
   * A column of a migration: the same column, by its id, in both schemas.
   *
   * @param table the table of the column
   * @param name the name of the column in the statement that changes it
   * @param id the id of the column
   */
  public record ColumnTransition(TableTransition table, String name, String id) {

    public ColumnTransition {
      Objects.requireNonNull(table, "table");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(id, "id");
    }

    /**
     * Gives the column before the migration.
     *
     * @return the column, or empty if the migration adds it
     */
    public Optional<Column> origin() {
      return table.origin().flatMap(origin -> origin.columnById(id));
    }

    /**
     * Gives the column after the migration, which a change of the column needs.
     *
     * @return the column
     * @throws InvalidSchemaException if the migration drops the column
     */
    public Column definition() {
      return table.definition().columnById(id).orElseThrow(() -> new InvalidSchemaException("The column " + table.name()
          + "." + name + " is not in the schema after the migration."));
    }
  }
}
