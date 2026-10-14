package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.command.Command;

/** Defers foreign key checks to the end of the transaction, while rebuilds of tables that no table references run. */
class SqliteDeferForeignKeysCommand extends Command {

  @Override
  public void render(ScriptWriter sql) {
    sql.append("PRAGMA defer_foreign_keys = ON");
  }
}
