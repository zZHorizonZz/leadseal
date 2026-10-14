package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.DefaultAttribute;

/**
 * Sets the default of a column.
 */
public class SetDefaultCommand extends Command {

  private final TableName table;
  private final String column;
  private final DefaultAttribute value;

  public SetDefaultCommand(TableName table, String column, DefaultAttribute value) {
    this.table = table;
    this.column = column;
    this.value = value;
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

  /**
   * Returns the default.
   *
   * @return the default
   */
  public DefaultAttribute value() {
    return value;
  }

  @Override
  public void render(ScriptWriter sql) {
    Column after = sql.columnAfter(table, column);
    sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column)
        .append(" SET DEFAULT " + sql.dialect().defaultValue(after, value));
  }
}
