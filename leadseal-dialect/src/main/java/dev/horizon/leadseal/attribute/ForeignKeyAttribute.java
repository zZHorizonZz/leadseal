package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Renaming;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.change.Change;
import java.util.List;

/**
 * A foreign key of a table.
 *
 * @param name the constraint name
 * @param columns the columns of this table
 * @param table the referenced table
 * @param referencedColumns the referenced columns, which must be the primary key or a unique index of that table
 * @param onDelete what happens to the rows of this table when the referenced row is deleted
 * @param onUpdate what happens to the rows of this table when the referenced key changes
 */
public record ForeignKeyAttribute(String name, List<String> columns, TableName table, List<String> referencedColumns,
    Action onDelete, Action onUpdate) implements TableAttribute {

  public ForeignKeyAttribute {
    if (name == null || name.isBlank()) {
      throw new SchemaException("A foreign key has no name.");
    }
    if (table == null) {
      throw new SchemaException("The foreign key '" + name + "' references no table.");
    }
    columns = List.copyOf(columns);
    referencedColumns = List.copyOf(referencedColumns);
    if (columns.isEmpty() || columns.size() != referencedColumns.size()) {
      throw new SchemaException("The foreign key '" + name + "' must have as many columns as it references, and at"
          + " least one.");
    }
    onDelete = onDelete == null ? Action.NO_ACTION : onDelete;
    onUpdate = onUpdate == null ? Action.NO_ACTION : onUpdate;
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
  public static ForeignKeyAttribute of(String table, List<String> columns, TableName referencedTable,
      List<String> referencedColumns, Action onDelete) {
    return new ForeignKeyAttribute(new ConstraintName("fk", table, columns).value(), columns, referencedTable,
        referencedColumns, onDelete, Action.NO_ACTION);
  }

  /**
   * Returns a copy of this foreign key with another name.
   *
   * @param name the constraint name
   * @return the new foreign key
   */
  public ForeignKeyAttribute withName(String name) {
    return new ForeignKeyAttribute(name, columns, table, referencedColumns, onDelete, onUpdate);
  }

  /**
   * Returns a copy of this foreign key with another update action.
   *
   * @param onUpdate what happens when the referenced key changes
   * @return the new foreign key
   */
  public ForeignKeyAttribute withOnUpdate(Action onUpdate) {
    return new ForeignKeyAttribute(name, columns, table, referencedColumns, onDelete, onUpdate);
  }

  @Override
  public Object key() {
    return List.of(ForeignKeyAttribute.class, name);
  }

  @Override
  public String summary(String owner) {
    return name;
  }

  @Override
  public void validate(Table owner) {
    for (String column : columns) {
      Column found = owner.column(column).orElseThrow(() -> new SchemaException("The foreign key '" + name + "'"
          + " refers to the column '" + column + "', which the table '" + owner.name() + "' does not have."));
      if (found.nullable() || !onDelete.setsNull() && !onUpdate.setsNull()) {
        continue;
      }
      throw new SchemaException("The foreign key '" + name + "' sets " + owner.name() + "." + column + " to NULL,"
          + " but the column does not accept NULL.");
    }
  }

  /**
   * Checks that the referenced table exists, and that the referenced columns are its primary key or a unique index
   * with columns of the same types.
   *
   * @param owner the table that has this foreign key
   * @param schema the schema of both tables
   * @throws SchemaException if the reference is invalid
   */
  public void validate(Table owner, Schema schema) {
    Table target = schema.table(table).orElseThrow(() -> new SchemaException("The foreign key '" + name + "'"
        + " references the table '" + table + "', which the schema does not have."));
    boolean unique = target.primaryKey().columns().equals(referencedColumns)
        || target.attributes(IndexAttribute.class).stream()
            .anyMatch(index -> index.unique() && index.where() == null && index.columns().equals(referencedColumns));
    if (!unique) {
      throw new SchemaException("The foreign key '" + name + "' references " + target.name() + referencedColumns
          + ", which is neither the primary key nor a unique index of that table.");
    }
    for (int i = 0; i < columns.size(); i++) {
      Column column = owner.column(columns.get(i)).orElseThrow();
      Column referenced = target.column(referencedColumns.get(i)).orElseThrow();
      if (!column.type().equals(referenced.type())) {
        throw new SchemaException("The foreign key '" + name + "' joins " + owner.name() + "." + column.name() + " ("
            + column.type() + ") with " + target.name() + "." + referenced.name() + " (" + referenced.type()
            + "). The types must be the same.");
      }
    }
  }

  @Override
  public TableAttribute renamed(TableName owner, Renaming renaming) {
    return new ForeignKeyAttribute(name, renaming.columns(owner, columns), renaming.table(table),
        renaming.columns(table, referencedColumns), onDelete, onUpdate);
  }

  /** What happens to referencing rows when the referenced row is deleted or its key changes. */
  public enum Action {
    /** The change fails at the end of the statement. */
    NO_ACTION,
    /** The change fails at once. */
    RESTRICT,
    /** The referencing rows are deleted or updated too. */
    CASCADE,
    /** The referencing columns are set to {@code NULL}. */
    SET_NULL;

    /**
     * Returns whether this action sets the referencing columns to {@code NULL}.
     *
     * @return {@code true} for {@link #SET_NULL}
     */
    public boolean setsNull() {
      return this == SET_NULL;
    }
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ADD_FOREIGN_KEYS;
  }

  @Override
  public Change.Phase dropPhase() {
    return Change.Phase.DROP_FOREIGN_KEYS;
  }
}
