package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.MoveTableCommand;

/** {@code RENAME TABLE} into another database, which is created first if it does not exist. */
class MariaDbMoveTableCommand extends MoveTableCommand {

  MariaDbMoveTableCommand(TableName from, String to) {
    super(from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    if (to() != null) {
      sql.render(sql.commands().createSchema(to())).end();
    }
    sql.append("RENAME TABLE ").table(from()).append(" TO ").table(from().inNamespace(to()));
  }
}
