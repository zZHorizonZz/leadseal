package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.change.Change;
import java.util.List;

/**
 * The statements that a database runs for each change of a migration: its data definition language. A dialect
 * composes it from the statements of tables, of columns, of constraints and of comments.
 *
 * <p>
 * The default methods of each part give the statements of standard SQL. A dialect gives its own part where its
 * database writes some statements in another way, or writes the statements that standard SQL has none for, such as
 * moving a table to another namespace. A method gives no commands for a change that the database makes in another
 * statement, or that it does not need.
 */
public interface Ddl {

  /**
   * Gives the dialect of these statements.
   *
   * @return the dialect
   */
  Dialect dialect();

  /**
   * Gives the statements of tables and namespaces.
   *
   * @return the statements
   */
  TableDdl table();

  /**
   * Gives the statements of columns.
   *
   * @return the statements
   */
  ColumnDdl column();

  /**
   * Gives the statements of primary keys, indexes, checks and foreign keys.
   *
   * @return the statements
   */
  ConstraintDdl constraint();

  /**
   * Gives the statements of comments.
   *
   * @return the statements
   */
  CommentDdl comment();

  /**
   * Gives the commands of the changes of a migration. By default, each change gives its commands, and a command that
   * an earlier change gave already runs once.
   *
   * @param changes the changes, in the order they run
   * @param transition the schemas before and after the migration
   * @return the commands, in the order they run
   */
  default List<Command> plan(List<Change> changes, Transition transition) {
    return changes.stream().flatMap(change -> change.commands(this, transition).stream()).distinct().toList();
  }
}
