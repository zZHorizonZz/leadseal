package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * Renames a column and keeps its values.
 */
public class RenameColumnCommand extends Command {

  private final TableName table;
  private final String from;
  private final String to;

  public RenameColumnCommand(TableName table, String from, String to) {
    this.table = table;
    this.from = from;
    this.to = to;
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
   * Returns the old column name.
   *
   * @return the old column name
   */
  public String from() {
    return from;
  }

  /**
   * Returns the new column name.
   *
   * @return the new column name
   */
  public String to() {
    return to;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table).append(" RENAME COLUMN ").name(from).append(" TO ").name(to);
  }
}
