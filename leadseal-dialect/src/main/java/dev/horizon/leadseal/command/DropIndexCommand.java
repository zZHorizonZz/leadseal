package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.IndexAttribute;

/**
 * {@code DROP INDEX}.
 */
public class DropIndexCommand extends Command {

  private final TableName table;
  private final IndexAttribute index;

  public DropIndexCommand(TableName table, IndexAttribute index) {
    this.table = table;
    this.index = index;
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
   * Returns the index.
   *
   * @return the index
   */
  public IndexAttribute index() {
    return index;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("DROP INDEX ").qualified(table.namespace(), index.name());
  }
}
