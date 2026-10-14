package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.command.SetDefaultCommand;

/** Replaces the default constraint of a column: drops the existing one, if any, and adds the new one. */
class SqlServerSetDefaultCommand extends SetDefaultCommand {

  SqlServerSetDefaultCommand(TableName table, String column, DefaultAttribute value) {
    super(table, column, value);
  }

  @Override
  public void render(ScriptWriter sql) {
    Column after = sql.columnAfter(table(), column());
    sql.render(sql.commands().dropDefault(table(), column())).end();
    sql.append("ALTER TABLE ").table(table()).append(" ADD CONSTRAINT ")
        .name(new SqlServerDefaultName(table().name(), column()).value())
        .append(" DEFAULT " + sql.dialect().defaultValue(after, value()) + " FOR ").name(column());
  }
}
