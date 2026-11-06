package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
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
 * A foreign key of a table.
 *
 * @param name the constraint name
 * @param columns the columns of this table
 * @param table the referenced table
 * @param referencedColumns the referenced columns, which must be the primary key or a unique index of that table
 * @param onDelete what happens to the rows of this table when the referenced row is deleted
 * @param onUpdate what happens to the rows of this table when the referenced key changes
 */
public record ForeignKey(String name, List<String> columns, TableName table, List<String> referencedColumns,
    Action onDelete, Action onUpdate) implements TableAttribute {

  public ForeignKey {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("A foreign key has no name.");
    }
    if (table == null) {
      throw new InvalidSchemaException("The foreign key '" + name + "' references no table.");
    }
    columns = List.copyOf(columns);
    referencedColumns = List.copyOf(referencedColumns);
    if (columns.isEmpty() || columns.size() != referencedColumns.size()) {
      throw new InvalidSchemaException("The foreign key '" + name + "' must have as many columns as it references, and at"
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
  public static ForeignKey of(String table, List<String> columns, TableName referencedTable,
      List<String> referencedColumns, Action onDelete) {
    return new ForeignKey(new ConstraintName("fk", table, columns).value(), columns, referencedTable,
        referencedColumns, onDelete, Action.NO_ACTION);
  }

  /**
   * Returns a copy of this foreign key with another name.
   *
   * @param name the constraint name
   * @return the new foreign key
   */
  public ForeignKey withName(String name) {
    return new ForeignKey(name, columns, table, referencedColumns, onDelete, onUpdate);
  }

  /**
   * Returns a copy of this foreign key with another update action.
   *
   * @param onUpdate what happens when the referenced key changes
   * @return the new foreign key
   */
  public ForeignKey withOnUpdate(Action onUpdate) {
    return new ForeignKey(name, columns, table, referencedColumns, onDelete, onUpdate);
  }

  @Override
  public Object key() {
    return List.of(ForeignKey.class, name);
  }

  @Override
  public String summary(String owner) {
    return name;
  }

  @Override
  public void validate(Table owner) {
    for (String columnName : columns) {
      Column column = owner.column(columnName).orElseThrow(() -> new InvalidSchemaException("The foreign key '" + name
          + "' refers to the column '" + columnName + "', which the table '" + owner.name() + "' does not have."));
      if (column.nullable() || !onDelete.setsNull() && !onUpdate.setsNull()) {
        continue;
      }
      throw new InvalidSchemaException("The foreign key '" + name + "' sets " + owner.name() + "." + column + " to NULL,"
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
  @Override
  public void validate(Table owner, Schema schema) {
    Table target = schema.table(table).orElseThrow(() -> new InvalidSchemaException("The foreign key '" + name + "'"
        + " references the table '" + table + "', which the schema does not have."));
    boolean unique = target.primaryKey().columns().equals(referencedColumns)
        || target.attributes(Index.class).stream()
            .anyMatch(index -> index.unique() && !index.rows().partial() && index.columns().equals(referencedColumns));
    if (!unique) {
      throw new InvalidSchemaException("The foreign key '" + name + "' references " + target.name() + referencedColumns
          + ", which is neither the primary key nor a unique index of that table.");
    }
    for (int i = 0; i < columns.size(); i++) {
      Column column = owner.column(columns.get(i)).orElseThrow();
      Column targetColumn = target.column(referencedColumns.get(i)).orElseThrow();
      if (!column.type().equals(targetColumn.type())) {
        throw new InvalidSchemaException("The foreign key '" + name + "' joins " + owner.name() + "." + column.name() + " ("
            + column.type() + ") with " + target.name() + "." + targetColumn.name() + " (" + targetColumn.type()
            + "). The types must be the same.");
      }
    }
  }

  @Override
  public Optional<TableAttribute> rename(TableName owner, Transition transition) {
    return transition.targetColumns(owner, columns).flatMap(targetColumns -> transition.targetName(table)
        .flatMap(targetTable -> transition.targetColumns(table, referencedColumns)
            .map(targetReferences -> new ForeignKey(name, targetColumns, targetTable, targetReferences, onDelete, onUpdate))));
  }

  /** What happens to referencing rows when the referenced row is deleted or its key changes. */
  public enum Action {
    /** The change fails at the end of the statement. SQL writes it by leaving the action out. */
    NO_ACTION("NO ACTION") {
      @Override
      public String clause(String event) {
        return "";
      }
    },
    /** The change fails at once. */
    RESTRICT("RESTRICT") {
      @Override
      public Action withoutRestrict() {
        return NO_ACTION;
      }
    },
    /** The referencing rows are deleted or updated too. */
    CASCADE("CASCADE"),
    /** The referencing columns are set to {@code NULL}. */
    SET_NULL("SET NULL");

    private final String keyword;

    Action(String keyword) {
      this.keyword = keyword;
    }

    /**
     * Writes this action for an event of a foreign key.
     *
     * @param event the event, {@code ON DELETE} or {@code ON UPDATE}
     * @return the SQL, for example {@code " ON DELETE CASCADE"}, or an empty text for {@link #NO_ACTION}
     */
    public String clause(String event) {
      return " " + event + " " + keyword;
    }

    /**
     * Returns the action for a database that has no {@code RESTRICT} and checks {@code NO ACTION} at once, as SQL
     * Server does.
     *
     * @return {@link #NO_ACTION} for {@link #RESTRICT}, else this action
     */
    public Action withoutRestrict() {
      return this;
    }

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

  @Override
  public List<Command> add(Ddl ddl, TableTransition table) {
    return ddl.constraint().addForeignKey(table, this);
  }

  @Override
  public List<Command> drop(Ddl ddl, TableTransition table) {
    return ddl.constraint().dropForeignKey(table, this);
  }

  @Override
  public boolean covers(String column) {
    return columns.contains(column);
  }
}
