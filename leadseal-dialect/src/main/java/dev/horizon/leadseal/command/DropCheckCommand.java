package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CheckAttribute;

/**
 * Drops a check constraint.
 */
public class DropCheckCommand extends Command {

  private final TableName table;
  private final CheckAttribute check;

  public DropCheckCommand(TableName table, CheckAttribute check) {
    this.table = table;
    this.check = check;
  }

  /**
   * Returns the table.
   *
   * @return the table
   */
  public TableName table() {
    return table;
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
    sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(check.name());
  }
}
