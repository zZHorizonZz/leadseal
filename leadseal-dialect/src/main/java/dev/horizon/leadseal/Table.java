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
 * @param name the table name
 * @param columns the columns, in order
 * @param primaryKey the primary key
 * @param indexes the indexes
 * @param foreignKeys the foreign keys
 */
public record Table(String id, String name, List<Column> columns, PrimaryKey primaryKey, List<Index> indexes,
    List<ForeignKey> foreignKeys) {

  public Table {
    Names.require(id, "table id");
    Names.require(name, "table name");
    columns = List.copyOf(columns);
    indexes = List.copyOf(indexes);
    foreignKeys = List.copyOf(foreignKeys);
    if (columns.isEmpty()) {
      throw new SchemaException("The table '" + name + "' has no columns.");
    }
    if (primaryKey == null) {
      throw new SchemaException("The table '" + name + "' has no primary key.");
    }
    unique(columns.stream().map(Column::name).toList(), "column name");
    unique(columns.stream().map(Column::id).toList(), "column id");
    unique(indexes.stream().map(Index::name).toList(), "index name");
    unique(foreignKeys.stream().map(ForeignKey::name).toList(), "foreign key name");
    Set<String> names = new HashSet<>(columns.stream().map(Column::name).toList());
    requireColumns(names, primaryKey.columns(), "The primary key");
    for (String key : primaryKey.columns()) {
      if (columns.stream().anyMatch(column -> column.name().equals(key) && column.nullable())) {
        throw new SchemaException("The primary key column '" + name + "." + key + "' must not be nullable.");
      }
    }
    for (Index index : indexes) {
      requireColumns(names, index.columns(), "The index '" + index.name() + "'");
    }
    for (ForeignKey key : foreignKeys) {
      requireColumns(names, key.columns(), "The foreign key '" + key.name() + "'");
      if (key.onDelete() == ForeignKey.Action.SET_NULL) {
        for (String column : key.columns()) {
          if (columns.stream().anyMatch(other -> other.name().equals(column) && !other.nullable())) {
            throw new SchemaException("The foreign key '" + key.name() + "' sets " + name + "." + column + " to NULL"
                + " on delete, but the column does not accept NULL.");
          }
        }
      }
    }
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

  private void requireColumns(Set<String> names, List<String> used, String owner) {
    for (String column : used) {
      if (!names.contains(column)) {
        throw new SchemaException(owner + " refers to the column '" + column + "', which the table '" + name
            + "' does not have.");
      }
    }
  }

  private void unique(List<String> values, String what) {
    Set<String> seen = new HashSet<>();
    for (String value : values) {
      if (!seen.add(value)) {
        throw new SchemaException("The table '" + name + "' has the " + what + " '" + value + "' twice.");
      }
    }
  }
}
