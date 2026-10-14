package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.command.ForeignKeyConstraintCommand;

/**
 * A foreign key constraint. SQL Server has no {@code RESTRICT}. A restricting foreign key uses the default,
 * {@code NO ACTION}, which also rejects the change.
 */
class SqlServerForeignKeyConstraintCommand extends ForeignKeyConstraintCommand {

  SqlServerForeignKeyConstraintCommand(ForeignKeyAttribute key) {
    super(key);
  }

  @Override
  protected String action(String event, ForeignKeyAttribute.Action action) {
    return action == ForeignKeyAttribute.Action.RESTRICT ? "" : super.action(event, action);
  }
}
