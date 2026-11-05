package dev.horizon.leadseal.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The commands that a dialect writes for the changes of a migration. A {@link Ddl} gives a {@link DefaultPlan}, or a
 * plan of its own when its database cannot make every change in place, as SQLite rebuilds tables.
 */
public interface Plan {

  /**
   * Gives the commands, in the order they run.
   *
   * @return the commands
   */
  List<Command> commands();

  /**
   * Gives commands with each {@linkplain Command#mergeKey() merge key} once, at its first place. Commands without a
   * merge key all stay.
   *
   * @param commands the commands, in the order they run
   * @return the commands, in the same order
   */
  default List<Command> merge(List<Command> commands) {
    Set<Object> keys = new HashSet<>();
    return commands.stream().filter(command -> command.mergeKey().map(keys::add).orElse(true)).toList();
  }
}
