package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.attribute.CheckAttribute;

/**
 * The definition of a check constraint: {@code CONSTRAINT name CHECK (expression)}.
 */
public class CheckConstraintCommand extends Command {

  private final CheckAttribute check;

  public CheckConstraintCommand(CheckAttribute check) {
    this.check = check;
  }

  /**
   * Returns the check.
   *
   * @return the check
   */
  public CheckAttribute check() {
    return check;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("CONSTRAINT ").name(check.name()).append(" CHECK (" + check.expression() + ")");
  }
}
