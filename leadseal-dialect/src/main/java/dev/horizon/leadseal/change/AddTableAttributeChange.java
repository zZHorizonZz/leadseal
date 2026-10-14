package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.TableAttribute;

/**
 * Adds an attribute to a table, such as an index or a foreign key.
 *
 * @param table the table
 * @param attribute the attribute
 */
public record AddTableAttributeChange(TableName table, TableAttribute attribute) implements Change {

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
}
