package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * Sets the collation of a text column in this database, or returns it to the database default.
 */
public class SetCollationCommand extends Command {

  private final TableName table;
  private final String column;
  private final String collation;

  public SetCollationCommand(TableName table, String column, String collation) {
    this.table = table;
    this.column = column;
    this.collation = collation;
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
   * Returns the collation, or {@code null} for the database default.
   *
   * @return the collation, or {@code null} for the database default
   */
  public String collation() {
    return collation;
  }

  @Override
  public void render(ScriptWriter sql) {
    Column after = sql.columnAfter(table, column);
    String type = sql.dialect().type(sql.tableAfter(table), after);
    sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column).append(" TYPE " + type);
    if (collation != null) {
      sql.append(" COLLATE ").name(collation);
    }
  }
}
