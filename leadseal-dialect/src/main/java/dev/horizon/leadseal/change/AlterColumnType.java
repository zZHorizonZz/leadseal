package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Changes the type of a column and keeps its values.
 *
 * @param table the table
 * @param column the name of the column
 * @param origin the type before
 * @param target the type after
 */
public record AlterColumnType(TableName table, String column, @JsonProperty("from") Type origin,
    @JsonProperty("to") Type target) implements TableChange {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.ALTER_COLUMN_TYPES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    TableTransition table = table(transition);
    Optional<Check> originCheck = ddl.dialect().typeCheck(table.name().name(), column, origin);
    Optional<Check> targetCheck = ddl.dialect().typeCheck(table.name().name(), column, target);
    List<Command> all = new ArrayList<>();
    if (!originCheck.equals(targetCheck)) {
      originCheck.ifPresent(check -> all.addAll(ddl.constraint().dropCheck(table, check)));
    }
    all.addAll(ddl.column().alterColumnType(table.column(column)));
    if (!originCheck.equals(targetCheck)) {
      targetCheck.ifPresent(check -> all.addAll(ddl.constraint().addCheck(table, check)));
    }
    return all;
  }

  @Override
  public Optional<String> warning() {
    return target.isAssignableFrom(origin) ? Optional.empty()
        : Optional.of("changes the column " + table + "." + column + " from " + origin + " to " + target
            + ", which can lose values");
  }

  @Override
  public void validate() {
    if (!target.isAssignableFrom(origin)) {
      throw new LossyTypeChangeException(table, column, origin, target);
    }
  }
}
