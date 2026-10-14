package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.CommandFactory;

/**
 * One change between two schemas. {@link Schema#changesTo} finds the changes, a migration's {@code migration.json}
 * stores them, and a dialect's {@link CommandFactory} turns them into commands.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "change")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateTableChange.class, name = "create_table"),
    @JsonSubTypes.Type(value = DropTableChange.class, name = "drop_table"),
    @JsonSubTypes.Type(value = RenameTableChange.class, name = "rename_table"),
    @JsonSubTypes.Type(value = MoveTableChange.class, name = "move_table"),
    @JsonSubTypes.Type(value = AddColumnChange.class, name = "add_column"),
    @JsonSubTypes.Type(value = DropColumnChange.class, name = "drop_column"),
    @JsonSubTypes.Type(value = RenameColumnChange.class, name = "rename_column"),
    @JsonSubTypes.Type(value = AlterColumnTypeChange.class, name = "alter_column_type"),
    @JsonSubTypes.Type(value = AddColumnAttributeChange.class, name = "add_column_attribute"),
    @JsonSubTypes.Type(value = DropColumnAttributeChange.class, name = "drop_column_attribute"),
    @JsonSubTypes.Type(value = AlterColumnAttributeChange.class, name = "alter_column_attribute"),
    @JsonSubTypes.Type(value = AddTableAttributeChange.class, name = "add_table_attribute"),
    @JsonSubTypes.Type(value = DropTableAttributeChange.class, name = "drop_table_attribute"),
    @JsonSubTypes.Type(value = AlterTableAttributeChange.class, name = "alter_table_attribute")
})
public sealed interface Change permits CreateTableChange, DropTableChange, RenameTableChange, MoveTableChange,
    AddColumnChange,
    DropColumnChange, RenameColumnChange, AlterColumnTypeChange, AddColumnAttributeChange, DropColumnAttributeChange,
    AlterColumnAttributeChange, AddTableAttributeChange, DropTableAttributeChange, AlterTableAttributeChange {

  /**
   * Returns the table that the change changes.
   *
   * @return the table name at the time the change runs. Drops of tables, indexes, checks and foreign keys run before
   * renames and use the old name. All other changes use the new name.
   */
  TableName table();

  /**
   * Returns the phase in which the change runs. A migration runs its changes phase by phase.
   *
   * @return the phase
   */
  Phase phase();

  /**
   * Returns a short description of the change, which names a migration.
   *
   * @return the description, for example {@code create_orders}
   */
  String summary();

  /**
   * Returns whether the change only supports other changes, such as a new index. A migration is named after its
   * changes that are not minor.
   *
   * @return {@code true} for a minor change
   */
  default boolean minor() {
    return false;
  }

  /**
   * The order in which the changes of a migration run. For example, foreign keys are dropped before their tables and
   * added after every table exists.
   */
  enum Phase {
    /** Drop the foreign keys of dropped tables, and changed or removed foreign keys. */
    DROP_FOREIGN_KEYS,
    /** Drop changed or removed checks. */
    DROP_CHECKS,
    /** Drop changed or removed indexes. */
    DROP_INDEXES,
    /** Drop tables. */
    DROP_TABLES,
    /** Rename tables. */
    RENAME_TABLES,
    /** Move tables to other namespaces. */
    MOVE_TABLES,
    /** Create tables. */
    CREATE_TABLES,
    /** Rename columns. */
    RENAME_COLUMNS,
    /** Add columns. */
    ADD_COLUMNS,
    /** Drop column attributes, such as defaults that the next phases would not accept. */
    DROP_COLUMN_ATTRIBUTES,
    /** Change column types. */
    ALTER_COLUMN_TYPES,
    /** Add or change column attributes. */
    ALTER_COLUMN_ATTRIBUTES,
    /** Add or change primary keys. */
    ALTER_PRIMARY_KEYS,
    /** Drop columns. */
    DROP_COLUMNS,
    /** Add indexes. */
    ADD_INDEXES,
    /** Add checks. */
    ADD_CHECKS,
    /** Add foreign keys. */
    ADD_FOREIGN_KEYS,
    /** Add, change or drop table comments. */
    COMMENT_TABLES
  }
}
