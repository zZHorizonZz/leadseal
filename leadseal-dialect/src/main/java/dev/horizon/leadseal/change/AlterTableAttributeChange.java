package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.TableAttribute;

/**
 * Replaces an attribute of a table with another attribute with the same key, such as another primary key.
 *
 * @param table the table
 * @param from the attribute before
 * @param to the attribute after
 */
public record AlterTableAttributeChange(TableName table, TableAttribute from, TableAttribute to) implements Change {

  @Override
  public String summary() {
    return "alter_" + to.summary(table.name());
  }

  @Override
  public boolean minor() {
    return to.minor();
  }

  @Override
  public Phase phase() {
    return to.phase();
  }
}
