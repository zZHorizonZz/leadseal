package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.command.CreateSchemaCommand;

/** Creates a schema if it does not exist. SQL Server has no {@code CREATE SCHEMA IF NOT EXISTS}. */
class SqlServerCreateSchemaCommand extends CreateSchemaCommand {

  SqlServerCreateSchemaCommand(String schema) {
    super(schema);
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("IF SCHEMA_ID(" + sql.dialect().string(schema()) + ") IS NULL EXEC("
        + sql.dialect().string("CREATE SCHEMA " + sql.dialect().quote(schema())) + ")");
  }
}
