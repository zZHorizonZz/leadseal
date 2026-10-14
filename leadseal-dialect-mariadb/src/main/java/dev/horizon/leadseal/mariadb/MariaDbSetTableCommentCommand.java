package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.command.SetTableCommentCommand;

/** {@code ALTER TABLE ... COMMENT}. An empty comment removes it. */
class MariaDbSetTableCommentCommand extends SetTableCommentCommand {

  MariaDbSetTableCommentCommand(TableName table, CommentAttribute from, CommentAttribute to) {
    super(table, from, to);
  }

  @Override
  public void render(ScriptWriter sql) {
    String comment = to() == null ? "" : to().text();
    sql.append("ALTER TABLE ").table(table()).append(" COMMENT = " + sql.dialect().string(comment));
  }
}
