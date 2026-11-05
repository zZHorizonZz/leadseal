package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.type.EnumType;
import java.util.List;

/**
 * The statements of the enum types of a dialect. A dialect without enum types writes each enum column with its own
 * type, so the defaults give no commands. They refuse a renamed value, because the columns store the value names.
 */
@FunctionalInterface
public interface TypeDdl {

  /**
   * Gives the statements of the dialect.
   *
   * @return the statements
   */
  Ddl ddl();

  /**
   * Gives the commands that create an enum type.
   *
   * @param type the enum type
   * @return the commands. The default gives none.
   */
  default List<Command> createType(EnumType type) {
    return List.of();
  }

  /**
   * Gives the commands that drop an enum type.
   *
   * @param type the enum type
   * @return the commands. The default gives none.
   */
  default List<Command> dropType(EnumType type) {
    return List.of();
  }

  /**
   * Gives the commands that change the values of an enum type.
   *
   * @param origin the enum type before the migration
   * @param target the enum type after the migration
   * @param transition the schemas before and after the migration
   * @return the commands. The default gives none.
   * @throws UnsupportedChangeException if a value keeps its number but gets another name
   */
  default List<Command> alterType(EnumType origin, EnumType target, Transition transition) {
    target.renames(origin).stream().findFirst().ifPresent(rename -> {
      throw new EnumRenameException(ddl().dialect(), target, rename);
    });
    return List.of();
  }
}
