package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommentDdl;
import java.util.List;

record PostgresCommentDdl(Dialect dialect) implements CommentDdl {

  @Override
  public List<Command> commentColumn(ColumnTransition column) {
    return List.of(new CommentOn("COLUMN " + dialect.quote(column.table().name()) + "."
        + dialect.quote(column.name()), column.definition().attribute(Comment.class).map(this::text).orElse("NULL")));
  }

  @Override
  public List<Command> commentTable(TableTransition table) {
    return List.of(new CommentOn("TABLE " + dialect.quote(table.name()),
        table.definition().attribute(Comment.class).map(this::text).orElse("NULL")));
  }

  private String text(Comment comment) {
    return dialect.string(comment.text());
  }

  private record CommentOn(String target, String text) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("COMMENT ON " + target + " IS " + text);
    }
  }
}
