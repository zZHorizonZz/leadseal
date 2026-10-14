package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.IndexAttribute;

/**
 * {@code CREATE INDEX} or {@code CREATE UNIQUE INDEX}, with its descending columns and the condition of a partial
 * index.
 */
public class AddIndexCommand extends Command {

  private final TableName table;
  private final IndexAttribute index;

  public AddIndexCommand(TableName table, IndexAttribute index) {
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
    sql.append(index.unique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ").name(index.name()).append(" ON ")
        .table(table).append(" (");
    for (int i = 0; i < index.columns().size(); i++) {
      String column = index.columns().get(i);
      sql.append(i == 0 ? "" : ", ").name(column).append(index.descending().contains(column) ? " DESC" : "");
    }
    sql.append(")");
    if (index.where() != null) {
      sql.append(" WHERE " + index.where());
    }
  }
}
