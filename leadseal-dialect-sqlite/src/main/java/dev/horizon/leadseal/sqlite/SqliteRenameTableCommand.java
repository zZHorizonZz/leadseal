package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.RenameTableCommand;

/** {@code ALTER TABLE ... RENAME TO}. SQLite cannot rename a constraint, so the primary key keeps its name. */
class SqliteRenameTableCommand extends RenameTableCommand {

  SqliteRenameTableCommand(TableName from, String to) {
    super(from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(from()).append(" RENAME TO ").name(to());
  }
}
