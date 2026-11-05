package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Renames a table and keeps its rows.
 *
 * @param origin the table
 * @param name the new name of the table
 */
public record RenameTable(@JsonProperty("from") TableName origin, @JsonProperty("to") String name) implements Change {

  @Override
  public TableName table() {
    return origin.withName(name);
  }

  @Override
  public String summary() {
    return "rename_" + origin.name() + "_to_" + name;
  }

  @Override
  public Phase phase() {
    return Phase.RENAME_TABLES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.table().renameTable(transition.originTable(origin), name);
  }
}
