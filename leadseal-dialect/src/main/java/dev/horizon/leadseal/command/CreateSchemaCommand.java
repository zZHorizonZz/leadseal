package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;

/**
 * Creates a database schema if it does not exist: {@code CREATE SCHEMA IF NOT EXISTS}.
 * Migrations never drop schemas.
 */
public class CreateSchemaCommand extends Command {

  private final String schema;

  public CreateSchemaCommand(String schema) {
    this.schema = schema;
  }

  /**
   * Returns the schema name.
   *
   * @return the schema name
   */
  public String schema() {
    return schema;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("CREATE SCHEMA IF NOT EXISTS ").name(schema);
  }
}
