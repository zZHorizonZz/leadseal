package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.EnumType;
import java.util.List;

/**
 * Changes the values of an enum type.
 *
 * @param origin the enum type before the migration
 * @param target the enum type after the migration, with the same name
 */
public record AlterEnumType(@JsonProperty("from") EnumType origin, @JsonProperty("to") EnumType target)
    implements
      TypeChange {

  @Override
  public Phase phase() {
    return Phase.ALTER_TYPES;
  }

  @Override
  public String summary() {
    return "alter_enum_" + target.name();
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.type().alterType(origin, target, transition);
  }
}
