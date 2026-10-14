package dev.horizon.leadseal;

import java.util.List;

/**
 * A foreign key of a table.
 *
 * @param name the constraint name
 * @param columns the columns of this table
 * @param table the referenced table
 * @param referencedColumns the referenced columns, which must be the primary key or a unique index of that table
 * @param onDelete what happens to the rows of this table when the referenced row is deleted
 * @param onUpdate what happens to the rows of this table when the referenced columns change
 */
public record ForeignKey(String name, List<String> columns, String table, List<String> referencedColumns,
    Action onDelete, Action onUpdate) {

  public ForeignKey {
    Names.require(name, "foreign key name");
    Names.require(table, "referenced table of the foreign key '" + name + "'");
    columns = List.copyOf(columns);
    referencedColumns = List.copyOf(referencedColumns);
    if (columns.isEmpty() || columns.size() != referencedColumns.size()) {
      throw new SchemaException("The foreign key '" + name + "' must have as many columns as it references, and at"
          + " least one.");
    }
    if (onDelete == null) {
      onDelete = Action.NO_ACTION;
    }
    if (onUpdate == null) {
      onUpdate = Action.NO_ACTION;
    }
  }

  public ForeignKey(String name, List<String> columns, String table, List<String> referencedColumns,
      Action onDelete) {
    this(name, columns, table, referencedColumns, onDelete, Action.NO_ACTION);
  }

  /**
   * Returns a foreign key with a generated name: {@code fk_} followed by the table name and the column names.
   *
   * @param table the name of the table that has the foreign key
   * @param columns the columns of that table
   * @param referencedTable the referenced table
   * @param referencedColumns the referenced columns
   * @param onDelete what happens when the referenced row is deleted
   * @return the foreign key
   */
  public static ForeignKey of(String table, List<String> columns, String referencedTable,
      List<String> referencedColumns, Action onDelete) {
    return new ForeignKey(Names.generate("fk", table, columns), columns, referencedTable, referencedColumns, onDelete);
  }

  /**
   * Returns a copy of this foreign key with another update action.
   *
   * @param onUpdate what happens when the referenced columns change
   * @return the new foreign key
   */
  public ForeignKey withOnUpdate(Action onUpdate) {
    return new ForeignKey(name, columns, table, referencedColumns, onDelete, onUpdate);
  }

  /**
   * Returns a copy of this foreign key with other columns and another referenced table.
   *
   * @param columns the columns of this table
   * @param table the referenced table
   * @param referencedColumns the referenced columns
   * @return the new foreign key
   */
  public ForeignKey withColumns(List<String> columns, String table, List<String> referencedColumns) {
    return new ForeignKey(name, columns, table, referencedColumns, onDelete, onUpdate);
  }

  /** What happens to referencing rows when the referenced row is deleted or its key changes. */
  public enum Action {
    /** The delete or update fails at the end of the statement. */
    NO_ACTION,
    /** The delete or update fails at once. */
    RESTRICT,
    /** The referencing rows are deleted or updated too. */
    CASCADE,
    /** The referencing columns are set to {@code NULL}. */
    SET_NULL
  }
}
