package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * Makes a column reject {@code NULL}. Rows with {@code NULL} get the default of the column, or the zero value of its
 * type, first.
 */
public class SetNotNullCommand extends Command {

  private final TableName table;
  private final String column;

  public SetNotNullCommand(TableName table, String column) {
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
    Column after = sql.columnAfter(table, column);
    sql.append("UPDATE ").table(table).append(" SET ").name(column).append(" = " + sql.dialect().fill(after) + " WHERE ")
        .name(column).append(" IS NULL").end();
    sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column).append(" SET NOT NULL");
  }
}
