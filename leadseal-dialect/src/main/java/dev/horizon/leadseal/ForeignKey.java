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
 */
public record ForeignKey(String name, List<String> columns, String table, List<String> referencedColumns,
    Action onDelete) {

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

  /** What happens to referencing rows when the referenced row is deleted. */
  public enum Action {
    /** The delete fails at the end of the statement. */
    NO_ACTION,
    /** The delete fails at once. */
    RESTRICT,
    /** The referencing rows are deleted too. */
    CASCADE,
    /** The referencing columns are set to {@code NULL}. */
    SET_NULL
  }
}
