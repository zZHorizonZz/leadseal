package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Moves a table to another namespace and keeps its rows.
 *
 * @param origin the table, with its new name in its old namespace
 * @param namespace the new namespace
 */
public record MoveTable(@JsonProperty("from") TableName origin, @JsonProperty("to") Namespace.Name namespace)
    implements
      Change {

  @Override
  public TableName table() {
    return origin.inNamespace(namespace);
  }

  @Override
  public String summary() {
    return "move_" + origin.name() + "_to_" + namespace;
  }

  @Override
  public Phase phase() {
    return Phase.MOVE_TABLES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.table().moveTable(origin, namespace);
  }
}
