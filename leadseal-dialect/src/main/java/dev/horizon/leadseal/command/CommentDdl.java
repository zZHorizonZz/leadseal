package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import java.util.List;

/** The statements of comments. Standard SQL has no comments, so each dialect writes them in its own way. */
public interface CommentDdl {

  /**
   * Gives the commands that give a column the comment that it has after the migration, or remove its comment.
   *
   * @param column the column
   * @return the commands
   */
  List<Command> commentColumn(ColumnTransition column);

  /**
   * Gives the commands that give a table the comment that it has after the migration, or remove its comment.
   *
   * @param table the table
   * @return the commands
   */
  List<Command> commentTable(TableTransition table);
}
