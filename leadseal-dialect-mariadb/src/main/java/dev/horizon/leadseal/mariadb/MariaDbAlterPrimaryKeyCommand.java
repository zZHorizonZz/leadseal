package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.command.AlterPrimaryKeyCommand;

/** {@code ALTER TABLE ... DROP PRIMARY KEY, ADD PRIMARY KEY}, in one statement. */
class MariaDbAlterPrimaryKeyCommand extends AlterPrimaryKeyCommand {

  MariaDbAlterPrimaryKeyCommand(TableName table, PrimaryKeyAttribute from, PrimaryKeyAttribute to) {
    super(table, from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table()).append(" DROP PRIMARY KEY, ADD PRIMARY KEY (").names(to().columns())
        .append(")");
  }
}
