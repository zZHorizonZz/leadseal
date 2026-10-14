package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;

/**
 * Drops a foreign key constraint.
 */
public class DropForeignKeyCommand extends Command {

  private final TableName table;
  private final ForeignKeyAttribute key;

  public DropForeignKeyCommand(TableName table, ForeignKeyAttribute key) {
    this.table = table;
    this.key = key;
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
   * Returns the foreign key.
   *
   * @return the foreign key
   */
  public ForeignKeyAttribute key() {
    return key;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(key.name());
  }
}
