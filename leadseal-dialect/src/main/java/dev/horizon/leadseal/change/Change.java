package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * One change between two schemas. {@link Schema#changesTo} finds the changes, a migration's {@code migration.json}
 * stores them, and each change gives the commands that make it in the {@link Ddl} of a dialect.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "change")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateTable.class, name = "create_table"),
    @JsonSubTypes.Type(value = DropTable.class, name = "drop_table"),
    @JsonSubTypes.Type(value = RenameTable.class, name = "rename_table"),
    @JsonSubTypes.Type(value = MoveTable.class, name = "move_table"),
    @JsonSubTypes.Type(value = AddColumn.class, name = "add_column"),
    @JsonSubTypes.Type(value = DropColumn.class, name = "drop_column"),
    @JsonSubTypes.Type(value = RenameColumn.class, name = "rename_column"),
    @JsonSubTypes.Type(value = AlterColumnType.class, name = "alter_column_type"),
    @JsonSubTypes.Type(value = AddColumnAttribute.class, name = "add_column_attribute"),
    @JsonSubTypes.Type(value = DropColumnAttribute.class, name = "drop_column_attribute"),
    @JsonSubTypes.Type(value = AlterColumnAttribute.class, name = "alter_column_attribute"),
    @JsonSubTypes.Type(value = AddTableAttribute.class, name = "add_table_attribute"),
    @JsonSubTypes.Type(value = DropTableAttribute.class, name = "drop_table_attribute"),
    @JsonSubTypes.Type(value = AlterTableAttribute.class, name = "alter_table_attribute")
})
public sealed interface Change permits CreateTable, DropTable, RenameTable, MoveTable, AddColumn, DropColumn,
    RenameColumn, AlterColumnType, AddColumnAttribute, DropColumnAttribute, AlterColumnAttribute, AddTableAttribute,
    DropTableAttribute, AlterTableAttribute {

  /**
   * Gives the table that the change changes.
   *
   * @return the table name at the time the change runs. Drops of tables, indexes, checks and foreign keys run before
   * renames and use the old name. All other changes use the new name.
   */
  TableName table();

  /**
   * Gives the table that the change changes, in the schemas of a migration.
   *
   * @param transition the schemas before and after the migration
   * @return the table, by its name after the migration, or before it for a change that runs before renames
   */
  default TableTransition table(Transition transition) {
    return phase().precedes(Phase.RENAME_TABLES) ? transition.originTable(table()) : transition.table(table());
  }

  /**
   * Gives the phase of the change. The changes of a migration run phase by phase.
   *
   * @return the phase
   */
  Phase phase();

  /**
   * Gives the part of a migration name that describes the change, for example {@code add_orders_note}.
   *
   * @return the summary
   */
  String summary();

  /**
   * Tells if the change is minor, such as a new index. A minor change names a migration only when the migration has
   * no other changes.
   *
   * @return true for a minor change
   */
  default boolean minor() {
    return false;
  }

  /**
   * Gives the commands that make this change.
   *
   * @param ddl the statements of the dialect
   * @param transition the schemas before and after the migration
   * @return the commands, in the order they run
   */
  List<Command> commands(Ddl ddl, Transition transition);

  /** The phases of a migration, in the order they run. */
  enum Phase {
    /** Drops foreign keys, before the tables and columns that they read. */
    DROP_FOREIGN_KEYS,
    /** Drops checks. */
    DROP_CHECKS,
    /** Drops indexes. */
    DROP_INDEXES,
    /** Drops tables. */
    DROP_TABLES,
    /** Renames tables. */
    RENAME_TABLES,
    /** Moves tables to other namespaces. */
    MOVE_TABLES,
    /** Creates tables, without their foreign keys. */
    CREATE_TABLES,
    /** Renames columns. */
    RENAME_COLUMNS,
    /** Adds columns. */
    ADD_COLUMNS,
    /** Drops attributes of columns. */
    DROP_COLUMN_ATTRIBUTES,
    /** Changes the types of columns. */
    ALTER_COLUMN_TYPES,
    /** Adds and changes attributes of columns. */
    ALTER_COLUMN_ATTRIBUTES,
    /** Changes primary keys. */
    ALTER_PRIMARY_KEYS,
    /** Drops columns. */
    DROP_COLUMNS,
    /** Adds indexes. */
    ADD_INDEXES,
    /** Adds checks. */
    ADD_CHECKS,
    /** Adds foreign keys, after all the tables and columns that they read. */
    ADD_FOREIGN_KEYS,
    /** Changes the comments of tables. */
    COMMENT_TABLES;

    /**
     * Tells if this phase runs before another phase.
     *
     * @param other the other phase
     * @return true if this phase runs first
     */
    public boolean precedes(Phase other) {
      return compareTo(other) < 0;
    }
  }

}
