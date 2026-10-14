package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.RenameColumnCommand;

/** Renames a column with {@code sp_rename}. */
class SqlServerRenameColumnCommand extends RenameColumnCommand {

  SqlServerRenameColumnCommand(TableName table, String from, String to) {
    super(table, from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    String column = sql.dialect().qualified(table()) + "." + sql.dialect().quote(from());
    sql.append("EXEC sp_rename " + sql.dialect().string(column) + ", " + sql.dialect().string(to()) + ", N'COLUMN'");
  }
}
