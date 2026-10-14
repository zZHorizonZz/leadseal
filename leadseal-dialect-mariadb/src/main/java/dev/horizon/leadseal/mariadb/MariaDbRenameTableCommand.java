package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.RenameTableCommand;

/**
 * {@code ALTER TABLE ... RENAME TO}, with the new name in the same database. MariaDB names every primary key
 * {@code PRIMARY}, so the key keeps its name.
 */
class MariaDbRenameTableCommand extends RenameTableCommand {

  MariaDbRenameTableCommand(TableName from, String to) {
    super(from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(from()).append(" RENAME TO ").table(from().withName(to()));
  }
}
