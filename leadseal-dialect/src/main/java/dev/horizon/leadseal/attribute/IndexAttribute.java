package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Renaming;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.change.Change;
import java.util.List;

/**
 * An index of a table.
 *
 * @param name the index name
 * @param columns the indexed columns, in order
 * @param unique {@code true} for an index that rejects two rows with the same values
 * @param descending the indexed columns that the index sorts in descending order
 * @param where the SQL condition of a partial index, which indexes only the rows that meet it, or {@code null} to
 * index every row
 */
public record IndexAttribute(String name, List<String> columns, boolean unique, List<String> descending,
    String where) implements TableAttribute {

  public IndexAttribute {
    if (name == null || name.isBlank()) {
      throw new SchemaException("An index has no name.");
    }
    columns = List.copyOf(columns);
    descending = descending == null ? List.of() : List.copyOf(descending);
    where = where == null || where.isBlank() ? null : where;
    if (columns.isEmpty()) {
      throw new SchemaException("The index '" + name + "' has no columns.");
    }
    for (String column : descending) {
      if (!columns.contains(column)) {
        throw new SchemaException("The index '" + name + "' sorts the column '" + column + "' in descending order,"
            + " but does not index it.");
      }
    }
  }

  /**
   * Returns an index with a generated name: {@code ix_} for an index or {@code ux_} for a unique index, followed by
   * the table name and the column names.
   *
   * @param table the table name
   * @param columns the indexed columns
   * @param unique {@code true} for a unique index
   * @return the index
   */
  public static IndexAttribute of(String table, List<String> columns, boolean unique) {
    return new IndexAttribute(new ConstraintName(unique ? "ux" : "ix", table, columns).value(), columns, unique,
        List.of(), null);
  }

  /**
   * Returns a copy of this index that sorts some columns in descending order.
   *
   * @param descending the columns to sort in descending order
   * @return the new index
   */
  public IndexAttribute withDescending(List<String> descending) {
    return new IndexAttribute(name, columns, unique, descending, where);
  }

  /**
   * Returns a copy of this index that indexes only the rows that meet a condition.
   *
   * @param where the SQL condition, or {@code null} to index every row
   * @return the new index
   */
  public IndexAttribute withWhere(String where) {
    return new IndexAttribute(name, columns, unique, descending, where);
  }

  /**
   * Returns whether a column is in this index.
   *
   * @param column the column name
   * @return {@code true} if the index contains the column
   */
  public boolean contains(String column) {
    return columns.contains(column);
  }

  @Override
  public Object key() {
    return List.of(IndexAttribute.class, name);
  }

  @Override
  public String summary(String table) {
    return name;
  }

  @Override
  public void validate(Table table) {
    for (String column : columns) {
      if (table.column(column).isEmpty()) {
        throw new SchemaException("The index '" + name + "' refers to the column '" + column + "', which the table '"
            + table.name() + "' does not have.");
      }
    }
  }

  @Override
  public TableAttribute renamed(TableName table, Renaming renaming) {
    return new IndexAttribute(name, renaming.columns(table, columns), unique, renaming.columns(table, descending),
        where);
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ADD_INDEXES;
  }

  @Override
  public Change.Phase dropPhase() {
    return Change.Phase.DROP_INDEXES;
  }
}
