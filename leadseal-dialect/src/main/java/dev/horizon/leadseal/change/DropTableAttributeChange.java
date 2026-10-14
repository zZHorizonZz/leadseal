package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.TableAttribute;

/**
 * Drops an attribute of a table.
 *
 * @param table the table
 * @param attribute the attribute before the change
 */
public record DropTableAttributeChange(TableName table, TableAttribute attribute) implements Change {

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
}
