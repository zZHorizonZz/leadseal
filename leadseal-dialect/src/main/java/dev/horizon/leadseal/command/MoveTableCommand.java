package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * Moves a table to another namespace, and creates that namespace if it does not exist.
 */
public class MoveTableCommand extends Command {

  private final TableName from;
  private final String to;

  public MoveTableCommand(TableName from, String to) {
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
   * Returns the new namespace, or {@code null} for the default namespace.
   *
   * @return the new namespace, or {@code null} for the default namespace
   */
  public String to() {
    return to;
  }

  @Override
  public void render(ScriptWriter sql) {
    if (to != null) {
      sql.render(sql.commands().createSchema(to)).end();
      sql.append("ALTER TABLE ").table(from).append(" SET SCHEMA ").name(to);
      return;
    }
    String source = sql.dialect().qualified(from);
    sql.append("DO $$ BEGIN EXECUTE format('ALTER TABLE " + source.replace("'", "''")
        + " SET SCHEMA %I', current_schema()); END $$");
  }
}
