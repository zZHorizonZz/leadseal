package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Adds an attribute to a table.
 *
 * @param table the table
 * @param attribute the attribute
 */
public record AddTableAttribute(TableName table, TableAttribute attribute) implements Change {

  @Override
  public String summary() {
    return "add_" + attribute.summary(table.name());
  }

  @Override
  public boolean minor() {
    return attribute.minor();
  }

  @Override
  public Phase phase() {
    return attribute.phase();
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return attribute.add(ddl, table(transition));
  }
}
