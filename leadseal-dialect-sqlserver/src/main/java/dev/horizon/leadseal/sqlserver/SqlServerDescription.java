package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;

/**
 * The {@code MS_Description} extended property of a table or a column, which SQL Server tools show as its comment.
 *
 * @param table the table
 * @param column the column, or {@code null} for the table itself
 */
record SqlServerDescription(TableName table, String column) {

  /**
   * Renders the statements that replace the description.
   *
   * @param sql the writer
   * @param from the description before, or {@code null}
   * @param to the description after, or {@code null} to remove it
   */
  void replace(ScriptWriter sql, String from, String to) {
    if (from != null) {
      SqlServerVariable schema = SqlServerVariable.of(sql, "schema");
      sql.append(declare(sql, schema) + " EXEC sp_dropextendedproperty @name = N'MS_Description', " + target(sql,
          schema)).end();
    }
    if (to != null) {
      SqlServerVariable schema = SqlServerVariable.of(sql, "schema");
      sql.append(declare(sql, schema) + " EXEC sp_addextendedproperty @name = N'MS_Description', @value = "
          + sql.dialect().string(to) + ", " + target(sql, schema)).end();
    }
  }

  private String declare(ScriptWriter sql, SqlServerVariable schema) {
    String value = table.namespace() == null ? "SCHEMA_NAME()" : sql.dialect().string(table.namespace());
    return "DECLARE " + schema.name() + " sysname = " + value;
  }

  private String target(ScriptWriter sql, SqlServerVariable schema) {
    return "@level0type = N'SCHEMA', @level0name = " + schema.name() + ", @level1type = N'TABLE', @level1name = "
        + sql.dialect().string(table.name())
        + (column == null ? "" : ", @level2type = N'COLUMN', @level2name = " + sql.dialect().string(column));
  }
}
