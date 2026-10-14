package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;

/**
 * {@code ALTER TABLE ... MODIFY COLUMN}, which sets the whole definition of a column at once: its type, nullability,
 * default, identity, collation and comment. When the column stops accepting {@code NULL}, rows with {@code NULL} get its
 * default or zero value first.
 */
class MariaDbModifyColumnCommand extends Command {

  private final TableName table;
  private final String column;

  MariaDbModifyColumnCommand(TableName table, String column) {
    this.table = table;
    this.column = column;
  }

  @Override
  public void render(ScriptWriter sql) {
    Column after = sql.columnAfter(table, column);
    boolean becomesNotNull = !after.nullable() && sql.columnBefore(table, column).map(Column::nullable).orElse(false);
    if (becomesNotNull) {
      sql.append("UPDATE ").table(table).append(" SET ").name(column).append(" = " + sql.dialect().fill(after)
          + " WHERE ").name(column).append(" IS NULL").end();
    }
    sql.append("ALTER TABLE ").table(table).append(" MODIFY COLUMN ")
        .render(sql.commands().columnDefinition(sql.tableAfter(table), after));
  }
}
