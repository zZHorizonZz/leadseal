package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;

/**
 * Adds a column. A column that does not accept {@code NULL} and has no default is added with the zero value of its
 * type as a default, which fills existing rows. That default is then dropped.
 */
public class AddColumnCommand extends Command {

  private final TableName table;
  private final Column column;

  public AddColumnCommand(TableName table, Column column) {
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
   * Returns the new column.
   *
   * @return the new column
   */
  public Column column() {
    return column;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("ALTER TABLE ").table(table).append(" ADD COLUMN ")
        .render(sql.commands().columnDefinition(sql.tableAfter(table), column));
    if (needsFill()) {
      sql.append(" DEFAULT " + sql.dialect().zero(column.type())).end();
      sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column.name()).append(" DROP DEFAULT");
    }
    sql.end();
    column.attribute(CommentAttribute.class)
        .ifPresent(comment -> sql.render(sql.commands().setColumnComment(table, column.name(), null, comment)));
  }

  /**
   * Returns whether existing rows need a value for the new column: it does not accept {@code NULL}, and has no default
   * or identity.
   *
   * @return {@code true} if the column needs a temporary default
   */
  protected boolean needsFill() {
    return !column.nullable() && column.attribute(DefaultAttribute.class).isEmpty()
        && column.attribute(IdentityAttribute.class).isEmpty();
  }
}
