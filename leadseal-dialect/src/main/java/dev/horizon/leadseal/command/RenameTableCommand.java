package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;

/**
 * Renames a table and its primary key constraint. The table stays in its database schema.
 */
public class RenameTableCommand extends Command {

  private final TableName from;
  private final String to;

  public RenameTableCommand(TableName from, String to) {
    this.from = from;
    this.to = to;
  }

  /**
   * Returns the table.
   *
   * @return the table
   */
  public TableName from() {
    return from;
  }

  /**
   * Returns the new table name.
   *
   * @return the new table name
   */
  public String to() {
    return to;
  }

  @Override
  public void render(ScriptWriter sql) {
    Table old = sql.tableBefore(from);
    sql.append("ALTER TABLE ").table(from).append(" RENAME TO ").name(to).end();
    String oldKey = old.primaryKey().name();
    String newKey = sql.after().tableById(old.id()).orElseThrow().primaryKey().name();
    if (!oldKey.equals(newKey)) {
      sql.append("ALTER TABLE ").table(from.withName(to)).append(" RENAME CONSTRAINT ").name(oldKey).append(" TO ")
          .name(newKey);
    }
  }
}
