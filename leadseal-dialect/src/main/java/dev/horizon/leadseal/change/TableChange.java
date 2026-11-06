package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;

/** A change of one table, such as a new column or a dropped index. */
public sealed interface TableChange extends Change
    permits CreateTable, DropTable, RenameTable, MoveTable, AddColumn, DropGeneratedColumn,
    DropColumn, RenameColumn, AlterColumnType, AddColumnAttribute, DropColumnAttribute, AlterColumnAttribute,
    AddTableAttribute, DropTableAttribute, AlterTableAttribute {

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

}
