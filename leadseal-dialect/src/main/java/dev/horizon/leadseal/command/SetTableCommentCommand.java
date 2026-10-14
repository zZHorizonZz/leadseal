package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CommentAttribute;

/**
 * Sets, changes or removes the comment of a table: {@code COMMENT ON TABLE}.
 */
public class SetTableCommentCommand extends Command {

  private final TableName table;
  private final CommentAttribute from;
  private final CommentAttribute to;

  public SetTableCommentCommand(TableName table, CommentAttribute from, CommentAttribute to) {
    this.table = table;
    this.from = from;
    this.to = to;
  }

  /**
   * Returns the table.
   *
   * @return the table
   */
  public TableName table() {
    return table;
  }

  /**
   * Returns the comment before, or {@code null}.
   *
   * @return the comment before, or {@code null}
   */
  public CommentAttribute from() {
    return from;
  }

  /**
   * Returns the comment after, or {@code null} to remove it.
   *
   * @return the comment after, or {@code null} to remove it
   */
  public CommentAttribute to() {
    return to;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("COMMENT ON TABLE ").table(table).append(" IS " + (to == null ? "NULL" : sql.dialect().string(to.text())));
  }
}
