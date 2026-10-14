package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;

/** A command without SQL, for a change that a database does not write, such as a comment on SQLite. */
public class EmptyCommand extends Command {

  @Override
  public void render(ScriptWriter sql) {
  }
}
