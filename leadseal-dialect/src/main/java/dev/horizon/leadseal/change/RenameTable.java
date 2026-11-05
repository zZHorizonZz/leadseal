package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.ArrayList;
import java.util.List;

/**
 * Renames a table and keeps its rows.
 *
 * @param origin the table
 * @param name the new name of the table
 */
public record RenameTable(@JsonProperty("from") TableName origin, @JsonProperty("to") String name) implements TableChange {

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
    TableTransition table = transition.originTable(origin);
    List<Command> all = new ArrayList<>(ddl.table().renameTable(table, name));
    TableTransition target = new TableTransition(transition, origin.withName(name), table.id());
    for (Column column : table.origin().orElseThrow().columns()) {
      ddl.dialect().typeCheck(origin.name(), column.name(), column.type()).ifPresent(check -> all.addAll(ddl.constraint()
          .renameCheck(target, check, ddl.dialect().typeCheck(name, column.name(), column.type()).orElseThrow())));
    }
    return all;
  }
}
