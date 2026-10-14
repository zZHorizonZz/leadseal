package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * {@code DROP TABLE}.
 */
public class DropTableCommand extends Command {

  private final TableName table;

  public DropTableCommand(TableName table) {
    this.table = table;
  }

  /**
   * Returns the table.
   *
   * @return the table
   */
  public TableName table() {
    return table;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("DROP TABLE ").table(table);
  }
}
