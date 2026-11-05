package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommentDdl;
import java.util.List;

record SqliteCommentDdl() implements CommentDdl {

  @Override
  public List<Command> commentColumn(ColumnTransition column) {
    return List.of();
  }

  @Override
  public List<Command> commentTable(TableTransition table) {
    return List.of();
  }
}
