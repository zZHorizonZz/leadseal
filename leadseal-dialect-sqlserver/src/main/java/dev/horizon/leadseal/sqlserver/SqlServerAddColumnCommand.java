package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.command.AddColumnCommand;

/**
 * {@code ALTER TABLE ... ADD}. A column that needs a value for existing rows gets a default constraint with the zero
 * value of its type, which is then dropped.
 */
class SqlServerAddColumnCommand extends AddColumnCommand {

  SqlServerAddColumnCommand(TableName table, Column column) {
    super(table, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    Column column = column();
    sql.append("ALTER TABLE ").table(table()).append(" ADD ")
        .render(sql.commands().columnDefinition(sql.tableAfter(table()), column));
    if (needsFill()) {
      String constraint = new SqlServerDefaultName(table().name(), column.name()).value();
      sql.append(" CONSTRAINT ").name(constraint).append(" DEFAULT " + sql.dialect().zero(column.type())).end();
      sql.append("ALTER TABLE ").table(table()).append(" DROP CONSTRAINT ").name(constraint);
    }
    sql.end();
    column.attribute(CommentAttribute.class)
        .ifPresent(comment -> sql.render(sql.commands().setColumnComment(table(), column.name(), null, comment)));
  }
}
