package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.command.CreateTableCommand;

/**
 * {@code CREATE TABLE} with the foreign keys inline, as SQLite cannot add them later, followed by the indexes. The
 * table is the one of the schema after the migration, with its foreign keys.
 */
class SqliteCreateTableCommand extends CreateTableCommand {

  SqliteCreateTableCommand(TableName table, Table definition) {
    super(table, definition);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.render(new SqliteTableStatementCommand(sql.tableAfter(table()), table())).end();
    for (IndexAttribute index : definition().attributes(IndexAttribute.class)) {
      sql.render(sql.commands().addIndex(table(), index)).end();
    }
  }
}
