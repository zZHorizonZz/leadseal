package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.RenameTableCommand;

/** Renames a table and its primary key constraint with {@code sp_rename}. */
class SqlServerRenameTableCommand extends RenameTableCommand {

  SqlServerRenameTableCommand(TableName from, String to) {
    super(from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    Table old = sql.tableBefore(from());
    sql.append("EXEC sp_rename " + sql.dialect().string(sql.dialect().qualified(from())) + ", "
        + sql.dialect().string(to())).end();
    String oldKey = old.primaryKey().name();
    String newKey = sql.after().tableById(old.id()).orElseThrow().primaryKey().name();
    if (!oldKey.equals(newKey)) {
      sql.append("EXEC sp_rename " + sql.dialect().string(sql.dialect().qualified(from().withName(oldKey))) + ", "
          + sql.dialect().string(newKey) + ", N'OBJECT'");
    }
  }
}
