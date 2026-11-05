package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.EnumType;
import java.util.List;

/**
 * Creates an enum type that a column of the target schema uses first.
 *
 * @param type the enum type
 */
public record CreateEnumType(EnumType type) implements TypeChange {

  @Override
  public Phase phase() {
    return Phase.CREATE_TYPES;
  }

  @Override
  public String summary() {
    return "create_enum_" + type.name();
  }

  @Override
  public boolean minor() {
    return true;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.type().createType(type);
  }
}
