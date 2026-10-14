package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.command.DropColumnCommand;

/** {@code ALTER TABLE ... DROP COLUMN}, after the default constraint of the column, which would block the drop. */
class SqlServerDropColumnCommand extends DropColumnCommand {

  SqlServerDropColumnCommand(TableName table, String column) {
    super(table, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    boolean hasDefault = sql.previous(table()).flatMap(old -> old.column(column()))
        .flatMap((Column old) -> old.attribute(DefaultAttribute.class)).isPresent();
    if (hasDefault) {
      sql.render(sql.commands().dropDefault(table(), column())).end();
    }
    super.render(sql);
  }
}
