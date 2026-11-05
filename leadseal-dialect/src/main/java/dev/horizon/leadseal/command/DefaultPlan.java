package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.change.Change;
import java.util.List;
import java.util.Objects;

/**
 * The plan of a dialect that makes every change in place, without rebuilding a table. Each change gives its commands,
 * in the order of the changes. A command with the same {@linkplain Command#mergeKey() merge key} as an earlier one does
 * not run again.
 *
 * @param ddl the statements of the dialect
 * @param changes the changes, in the order they run
 * @param transition the schemas before and after the migration
 */
public record DefaultPlan(Ddl ddl, List<Change> changes, Transition transition) implements Plan {

  public DefaultPlan {
    Objects.requireNonNull(ddl, "ddl");
    changes = List.copyOf(changes);
    Objects.requireNonNull(transition, "transition");
  }

  @Override
  public List<Command> commands() {
    return merge(changes.stream().flatMap(change -> change.commands(ddl, transition).stream()).toList());
  }
}
