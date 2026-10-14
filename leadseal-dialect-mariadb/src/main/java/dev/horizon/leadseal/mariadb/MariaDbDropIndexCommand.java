package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.command.DropIndexCommand;

/** {@code DROP INDEX ... ON}. */
class MariaDbDropIndexCommand extends DropIndexCommand {

  MariaDbDropIndexCommand(TableName table, IndexAttribute index) {
    super(table, index);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("DROP INDEX ").name(index().name()).append(" ON ").table(table());
  }
}
