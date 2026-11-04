package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommentDdl;
import java.util.List;

record MariaDbCommentDdl(MariaDbColumnDdl columns) implements CommentDdl {

  @Override
  public List<Command> commentColumn(ColumnTransition column) {
    return column.origin().isPresent() ? columns.modify(column) : List.of();
  }

  @Override
  public List<Command> commentTable(TableTransition table) {
    String text = table.definition().attribute(Comment.class).map(Comment::text).orElse("");
    return List.of(new TableComment(table.name(), columns.ddl().dialect().string(text)));
  }

  private record TableComment(TableName table, String text) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" COMMENT = " + text);
    }
  }
}
