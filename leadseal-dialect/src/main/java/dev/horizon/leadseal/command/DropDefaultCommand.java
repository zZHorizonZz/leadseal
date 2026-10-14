package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * Drops the default of a column.
 */
public class DropDefaultCommand extends Command {

  private final TableName table;
  private final String column;

  public DropDefaultCommand(TableName table, String column) {
    this.table = table;
    this.column = column;
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
   * Returns the column name.
   *
   * @return the column name
   */
  public String column() {
    return column;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column).append(" DROP DEFAULT");
  }
}
