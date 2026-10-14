package dev.horizon.leadseal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A table.
 *
 * <p>
 * The id identifies the table across schema versions. When a table keeps its id but changes its name, the diff renames
 * the table and keeps its rows. For a protobuf message, the id is the full message name.
 *
 * @param id the stable identity of the table
 * @param name the table name, unique across all database schemas
 * @param schema the database schema of the table, or {@code null} for the default schema of the connection
 * @param columns the columns, in order
 * @param primaryKey the primary key
 * @param indexes the indexes
 * @param foreignKeys the foreign keys
 * @param checks the check constraints
 * @param comment the comment, or {@code null} for none
 */
public record Table(String id, String name, String schema, List<Column> columns, PrimaryKey primaryKey,
    List<Index> indexes, List<ForeignKey> foreignKeys, List<Check> checks, String comment) {

  public Table {
    Names.require(id, "table id");
    Names.require(name, "table name");
    schema = schema == null || schema.isBlank() ? null : schema;
    comment = comment == null || comment.isBlank() ? null : comment;
    columns = List.copyOf(columns);
    indexes = indexes == null ? List.of() : List.copyOf(indexes);
    foreignKeys = foreignKeys == null ? List.of() : List.copyOf(foreignKeys);
    checks = checks == null ? List.of() : List.copyOf(checks);
    if (columns.isEmpty()) {
      throw new SchemaException("The table '" + name + "' has no columns.");
    }
    if (primaryKey == null) {
      throw new SchemaException("The table '" + name + "' has no primary key.");
    }
    unique(name, columns.stream().map(Column::name).toList(), "column name");
    unique(name, columns.stream().map(Column::id).toList(), "column id");
    unique(name, indexes.stream().map(Index::name).toList(), "index name");
    unique(name, foreignKeys.stream().map(ForeignKey::name).toList(), "foreign key name");
    unique(name, checks.stream().map(Check::name).toList(), "check name");
    Set<String> names = new HashSet<>(columns.stream().map(Column::name).toList());
    requireColumns(name, names, primaryKey.columns(), "The primary key");
    for (String key : primaryKey.columns()) {
      if (columns.stream().anyMatch(column -> column.name().equals(key) && column.nullable())) {
        throw new SchemaException("The primary key column '" + name + "." + key + "' must not be nullable.");
      }
    }
    for (Column column : columns) {
      if (column.identity() && !primaryKey.columns().equals(List.of(column.name()))) {
        throw new SchemaException("The identity column '" + name + "." + column.name() + "' must be the only column"
            + " of the primary key.");
      }
    }
    for (Index index : indexes) {
      requireColumns(name, names, index.columns(), "The index '" + index.name() + "'");
    }
    for (ForeignKey key : foreignKeys) {
      requireColumns(name, names, key.columns(), "The foreign key '" + key.name() + "'");
      if (key.onDelete() == ForeignKey.Action.SET_NULL || key.onUpdate() == ForeignKey.Action.SET_NULL) {
        for (String column : key.columns()) {
          if (columns.stream().anyMatch(other -> other.name().equals(column) && !other.nullable())) {
            throw new SchemaException("The foreign key '" + key.name() + "' sets " + name + "." + column
                + " to NULL, but the column does not accept NULL.");
          }
        }
      }
    }
  }

  public Table(String id, String name, List<Column> columns, PrimaryKey primaryKey, List<Index> indexes,
      List<ForeignKey> foreignKeys) {
    this(id, name, null, columns, primaryKey, indexes, foreignKeys, List.of(), null);
  }

  /**
   * Returns the column with a name.
   *
   * @param name the column name
   * @return the column, or an empty optional if the table has no column with that name
   */
  public Optional<Column> column(String name) {
    return columns.stream().filter(column -> column.name().equals(name)).findFirst();
  }

  /**
   * Returns the column with an id.
   *
   * @param id the column id
   * @return the column, or an empty optional if the table has no column with that id
   */
  public Optional<Column> columnById(String id) {
    return columns.stream().filter(column -> column.id().equals(id)).findFirst();
  }

  /**
   * Returns whether a column is part of the primary key, an index or a foreign key.
   *
   * @param name the column name
   * @return {@code true} if a key or an index contains the column
   */
  public boolean keyed(String name) {
    return primaryKey.columns().contains(name)
        || indexes.stream().anyMatch(index -> index.columns().contains(name))
        || foreignKeys.stream().anyMatch(key -> key.columns().contains(name));
  }

  /**
   * Returns a copy of this table in a database schema.
   *
   * @param schema the schema, or {@code null} for the default schema
   * @return the new table
   */
  public Table withSchema(String schema) {
    return new Table(id, name, schema, columns, primaryKey, indexes, foreignKeys, checks, comment);
  }

  /**
   * Returns a copy of this table with other check constraints.
   *
   * @param checks the checks
   * @return the new table
   */
  public Table withChecks(List<Check> checks) {
    return new Table(id, name, schema, columns, primaryKey, indexes, foreignKeys, checks, comment);
  }

  /**
   * Returns a copy of this table with another comment.
   *
   * @param comment the comment, or {@code null} for none
   * @return the new table
   */
  public Table withComment(String comment) {
    return new Table(id, name, schema, columns, primaryKey, indexes, foreignKeys, checks, comment);
  }

  private static void requireColumns(String table, Set<String> names, List<String> used, String owner) {
    for (String column : used) {
      if (!names.contains(column)) {
        throw new SchemaException(owner + " refers to the column '" + column + "', which the table '" + table
            + "' does not have.");
      }
    }
  }

  private static void unique(String table, List<String> values, String what) {
    Set<String> seen = new HashSet<>();
    for (String value : values) {
      if (!seen.add(value)) {
        throw new SchemaException("The table '" + table + "' has the " + what + " '" + value + "' twice.");
      }
    }
  }
}
