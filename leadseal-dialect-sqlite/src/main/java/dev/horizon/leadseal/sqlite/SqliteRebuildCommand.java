package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.command.Command;
import java.util.List;
import java.util.Optional;

/**
 * Rebuilds a table: creates the new table under a temporary name, copies the rows, drops the old table, renames the
 * new one and creates its indexes. Columns are matched by id. A new column gets its default, or {@code NULL} if it
 * accepts {@code NULL}, or the zero value of its type.
 */
class SqliteRebuildCommand extends Command {

  private final TableName from;
  private final TableName to;

  SqliteRebuildCommand(TableName from, TableName to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public void render(ScriptWriter sql) {
    Table before = sql.tableBefore(from);
    Table after = sql.tableAfter(to);
    TableName temporary = to.withName(sql.internal("new_" + to.name()));
    sql.render(new SqliteTableStatementCommand(after, temporary)).end();
    List<Column> copied = after.columns().stream()
        .filter(column -> before.columnById(column.id()).isPresent()
            || column.attribute(DefaultAttribute.class).isEmpty())
        .toList();
    sql.append("INSERT INTO ").table(temporary).append(" (").names(copied.stream().map(Column::name).toList())
        .append(") SELECT ");
    for (int i = 0; i < copied.size(); i++) {
      Column column = copied.get(i);
      sql.append(i == 0 ? "" : ", ");
      Optional<Column> old = before.columnById(column.id());
      if (old.isEmpty()) {
        sql.append(column.nullable() ? "NULL" : sql.dialect().zero(column.type()));
      } else if (!column.nullable() && old.get().nullable()) {
        sql.append("COALESCE(").name(old.get().name()).append(", " + sql.dialect().fill(column) + ")");
      } else {
        sql.name(old.get().name());
      }
    }
    sql.append(" FROM ").table(from).end();
    sql.append("DROP TABLE ").table(from).end();
    sql.append("ALTER TABLE ").table(temporary).append(" RENAME TO ").name(to.name()).end();
    for (IndexAttribute index : after.attributes(IndexAttribute.class)) {
      sql.render(sql.commands().addIndex(to, index)).end();
    }
  }
}
