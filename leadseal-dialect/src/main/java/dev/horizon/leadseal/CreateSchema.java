package dev.horizon.leadseal;

/**
 * Creates a database schema if it does not exist: {@code CREATE SCHEMA IF NOT EXISTS}. Tables and moves into a schema
 * render this node first. Migrations never drop schemas.
 *
 * @param schema the schema name
 */
public record CreateSchema(String schema) implements Node {

  @Override
  public void render(DdlWriter sql) {
    sql.append("CREATE SCHEMA IF NOT EXISTS ").name(schema);
  }
}
