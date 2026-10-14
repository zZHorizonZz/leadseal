package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;

/**
 * The DDL of PostgreSQL 13 and later. PostgreSQL writes every operation as standard SQL, so this dialect only maps
 * column types and values.
 */
public final class PostgresDialect implements Dialect {

  public static final String NAME = "postgresql";

  private static final int MAX_PRECISION = 1000;

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String string(String text) {
    return "'" + text.replace("'", "''") + "'";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case BOOLEAN -> "boolean";
      case INT -> "integer";
      case BIGINT -> "bigint";
      case REAL -> "real";
      case DOUBLE -> "double precision";
      case DECIMAL -> decimal(table, column);
      case TEXT -> column.length() == null ? "text" : "varchar(" + column.length() + ")";
      case BYTES -> "bytea";
      case UUID -> "uuid";
      case TIMESTAMP -> "timestamptz";
      case DATE -> "date";
      case TIME -> "time";
      case JSON -> "jsonb";
    };
  }

  @Override
  public String literal(Column column, String value) {
    return switch (column.type()) {
      case BOOLEAN, INT, BIGINT, REAL, DOUBLE, DECIMAL -> value;
      case BYTES -> string("\\x" + value);
      case TEXT, UUID, TIMESTAMP, DATE, TIME, JSON -> string(value);
    };
  }

  @Override
  public String generated(Column column, Default.Kind kind) {
    return switch (kind) {
      case NOW -> column.type() == ColumnType.DATE
          ? "((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)"
          : "CURRENT_TIMESTAMP";
      case RANDOM_UUID -> "gen_random_uuid()";
      case VALUE -> throw new IllegalArgumentException("A fixed default is a literal.");
    };
  }

  @Override
  public Decorator decorator() {
    return Decorator.none();
  }

  private static String decimal(Table table, Column column) {
    if (column.precision() > MAX_PRECISION) {
      throw new SchemaException("PostgreSQL cannot store the column " + table.name() + "." + column.name()
          + " with a precision of " + column.precision() + ". The maximum is " + MAX_PRECISION + ".");
    }
    return "numeric(" + column.precision() + ", " + (column.scale() == null ? 0 : column.scale()) + ")";
  }
}
