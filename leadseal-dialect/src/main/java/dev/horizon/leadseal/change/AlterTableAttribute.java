package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Changes an attribute of a table that can change in place, such as a primary key or a comment.
 *
 * @param table the table
 * @param origin the attribute before
 * @param target the attribute after, with the same key
 */
public record AlterTableAttribute(TableName table, @JsonProperty("from") TableAttribute origin,
    @JsonProperty("to") TableAttribute target) implements TableChange {

  @Override
  public String summary() {
    return "alter_" + target.summary(table.name());
  }

  @Override
  public boolean minor() {
    return target.minor();
  }

  @Override
  public Phase phase() {
    return target.phase();
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return target.alter(ddl, table(transition));
  }
}
