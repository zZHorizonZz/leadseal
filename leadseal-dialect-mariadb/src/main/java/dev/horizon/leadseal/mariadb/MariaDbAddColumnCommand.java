package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.command.AddColumnCommand;

/**
 * {@code ALTER TABLE ... ADD COLUMN}. A column that needs a value for existing rows gets the zero value of its type as
 * a default in its definition, which is then dropped.
 */
class MariaDbAddColumnCommand extends AddColumnCommand {

  MariaDbAddColumnCommand(TableName table, Column column) {
    super(table, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    Column added = needsFill() ? column().with(new LiteralDefaultAttribute(column().type().zero())) : column();
    sql.append("ALTER TABLE ").table(table()).append(" ADD COLUMN ")
        .render(sql.commands().columnDefinition(sql.tableAfter(table()), added));
    if (needsFill()) {
      sql.end();
      sql.append("ALTER TABLE ").table(table()).append(" ALTER COLUMN ").name(column().name()).append(" DROP DEFAULT");
    }
  }
}
