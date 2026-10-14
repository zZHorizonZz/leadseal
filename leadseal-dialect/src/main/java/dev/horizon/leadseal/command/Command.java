package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;

/**
 * A part of a migration script, such as {@code CREATE INDEX}, or a part of a statement, such as a column definition.
 *
 * <p>
 * The command classes of this package write standard SQL. A dialect writes its own SQL by extending a command class and
 * returning the subclass from its {@link CommandFactory}. A command renders other commands through the factory of the
 * writer, so that the dialect's subclasses render too.
 */
public abstract class Command {

  /**
   * Renders this command.
   *
   * @param sql the writer that receives the SQL
   */
  public abstract void render(ScriptWriter sql);
}
