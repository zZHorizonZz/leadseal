package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.command.DropForeignKeyCommand;

/** {@code ALTER TABLE ... DROP FOREIGN KEY}. */
class MariaDbDropForeignKeyCommand extends DropForeignKeyCommand {

  MariaDbDropForeignKeyCommand(TableName table, ForeignKeyAttribute key) {
    super(table, key);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table()).append(" DROP FOREIGN KEY ").name(key().name());
  }
}
