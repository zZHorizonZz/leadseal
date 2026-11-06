package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Triggers;
import java.util.List;

/**
 * The statements of triggers. Each database writes triggers in its own way, and standard SQL has no form that they
 * share, so each dialect gives its own.
 */
public interface TriggerDdl {

  /**
   * Gives the statements that these statements are part of.
   *
   * @return the statements of the dialect
   */
  Ddl ddl();

  /**
   * Gives the commands that create the database triggers of the rewrites and the triggers of a table, named
   * {@link Triggers#name} and {@link Triggers#rewriteName}.
   *
   * @param table the table, which has the triggers after the migration
   * @param triggers the rewrites and the triggers
   * @return the commands
   */
  List<Command> createTriggers(TableTransition table, Triggers triggers);

  /**
   * Gives the commands that drop the database triggers of the rewrites and the triggers of a table.
   *
   * @param table the table, which has the triggers before the migration
   * @param triggers the rewrites and the triggers
   * @return the commands
   */
  List<Command> dropTriggers(TableTransition table, Triggers triggers);
}
