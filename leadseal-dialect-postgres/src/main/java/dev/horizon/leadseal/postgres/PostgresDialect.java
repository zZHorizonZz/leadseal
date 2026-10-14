package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Table;

/**
 * The DDL of PostgreSQL 13 and later. PostgreSQL writes every operation as standard SQL, so this dialect only maps
 * column types.
 */
public final class PostgresDialect implements Dialect {

  public static final String NAME = "postgresql";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case BOOLEAN -> "boolean";
      case INT -> "integer";
      case BIGINT -> "bigint";
      case REAL -> "real";
      case DOUBLE -> "double precision";
      case TEXT -> column.length() == null ? "text" : "varchar(" + column.length() + ")";
      case BYTES -> "bytea";
      case UUID -> "uuid";
      case TIMESTAMP -> "timestamptz";
      case JSON -> "jsonb";
    };
  }

  @Override
  public String zero(Column column) {
    return switch (column.type()) {
      case BOOLEAN -> "false";
      case INT, BIGINT, REAL, DOUBLE -> "0";
      case TEXT, BYTES -> "''";
      case UUID -> "'00000000-0000-0000-0000-000000000000'";
      case TIMESTAMP -> "'1970-01-01 00:00:00+00'";
      case JSON -> "'{}'";
    };
  }

  @Override
  public Decorator decorator() {
    return Decorator.none();
  }
}
