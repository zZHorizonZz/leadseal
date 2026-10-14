package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.MoveTableCommand;

/** Moves a table to another schema with {@code ALTER SCHEMA ... TRANSFER}. */
class SqlServerMoveTableCommand extends MoveTableCommand {

  SqlServerMoveTableCommand(TableName from, String to) {
    super(from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    if (to() != null) {
      sql.render(sql.commands().createSchema(to())).end();
      sql.append("ALTER SCHEMA ").name(to()).append(" TRANSFER ").table(from());
      return;
    }
    String move = SqlServerVariable.of(sql, "move").name();
    sql.append("DECLARE " + move + " nvarchar(max) = N'ALTER SCHEMA ' + QUOTENAME(SCHEMA_NAME()) + N' TRANSFER ' + "
        + sql.dialect().string(sql.dialect().qualified(from())) + " EXEC(" + move + ")");
  }
}
