package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.EnumType;
import java.util.List;

/**
 * Drops an enum type that no column of the target schema uses.
 *
 * @param type the enum type
 */
public record DropEnumType(EnumType type) implements TypeChange {

  @Override
  public Phase phase() {
    return Phase.DROP_TYPES;
  }

  @Override
  public String summary() {
    return "drop_enum_" + type.name();
  }

  @Override
  public boolean minor() {
    return true;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.type().dropType(type);
  }
}
