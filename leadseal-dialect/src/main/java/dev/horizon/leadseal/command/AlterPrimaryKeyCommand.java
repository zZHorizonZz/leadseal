package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;

/**
 * Replaces the primary key of a table.
 */
public class AlterPrimaryKeyCommand extends Command {

  private final TableName table;
  private final PrimaryKeyAttribute from;
  private final PrimaryKeyAttribute to;

  public AlterPrimaryKeyCommand(TableName table, PrimaryKeyAttribute from, PrimaryKeyAttribute to) {
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
   * Returns the primary key before.
   *
   * @return the primary key before
   */
  public PrimaryKeyAttribute from() {
    return from;
  }

  /**
   * Returns the primary key after.
   *
   * @return the primary key after
   */
  public PrimaryKeyAttribute to() {
    return to;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(from.name()).end();
    sql.append("ALTER TABLE ").table(table).append(" ADD CONSTRAINT ").name(to.name()).append(" PRIMARY KEY (")
        .names(to.columns()).append(")");
  }
}
