package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.command.SetTableCommentCommand;

/** Sets the comment of a table as its {@code MS_Description} extended property. */
class SqlServerSetTableCommentCommand extends SetTableCommentCommand {

  SqlServerSetTableCommentCommand(TableName table, CommentAttribute from, CommentAttribute to) {
    super(table, from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    new SqlServerDescription(table(), null).replace(sql, from() == null ? null : from().text(),
        to() == null ? null : to().text());
  }
}
