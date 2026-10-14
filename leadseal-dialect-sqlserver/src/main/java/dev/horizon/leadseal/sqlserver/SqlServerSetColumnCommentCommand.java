package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.command.SetColumnCommentCommand;

/** Sets the comment of a column as its {@code MS_Description} extended property. */
class SqlServerSetColumnCommentCommand extends SetColumnCommentCommand {

  SqlServerSetColumnCommentCommand(TableName table, String column, CommentAttribute from, CommentAttribute to) {
    super(table, column, from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    new SqlServerDescription(table(), column()).replace(sql, from() == null ? null : from().text(),
        to() == null ? null : to().text());
  }
}
