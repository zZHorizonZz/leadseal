package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.AddColumnCommand;

/**
 * {@code ALTER TABLE ... ADD COLUMN}. A column that needs a value for existing rows keeps the zero value of its type
 * as its default, because SQLite cannot drop a default.
 */
class SqliteAddColumnCommand extends AddColumnCommand {

  SqliteAddColumnCommand(TableName table, Column column) {
    super(table, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table()).append(" ADD COLUMN ")
        .render(sql.commands().columnDefinition(sql.tableAfter(table()), column()));
    if (needsFill()) {
      sql.append(" DEFAULT " + sql.dialect().zero(column().type()));
    }
  }
}
