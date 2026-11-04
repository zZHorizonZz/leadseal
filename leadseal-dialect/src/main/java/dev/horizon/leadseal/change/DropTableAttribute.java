package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Drops an attribute of a table.
 *
 * @param table the table
 * @param attribute the attribute
 */
public record DropTableAttribute(TableName table, TableAttribute attribute) implements Change {

  @Override
  public String summary() {
    return "drop_" + attribute.summary(table.name());
  }

  @Override
  public boolean minor() {
    return attribute.minor();
  }

  @Override
  public Phase phase() {
    return attribute.dropPhase();
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return attribute.drop(ddl, table(transition));
  }
}
