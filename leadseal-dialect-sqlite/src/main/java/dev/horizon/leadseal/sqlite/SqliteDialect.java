package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Change;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Table;

/**
 * The DDL of SQLite 3.35 and later.
 *
 * <p>
 * SQLite cannot change a column, a primary key or a foreign key of an existing table. A migration with such a change
 * rebuilds the table: it creates the new table, copies the rows, drops the old table and renames the new one, as the
 * SQLite documentation describes. Column lengths are not enforced.
 */
public final class SqliteDialect implements Dialect {

  public static final String NAME = "sqlite";

  private final Decorator decorator = Decorator.of(Change.class, SqliteChange::new);

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
      case BOOLEAN, INT, BIGINT -> "INTEGER";
      case REAL, DOUBLE -> "REAL";
      case TEXT, UUID, TIMESTAMP, JSON -> "TEXT";
      case BYTES -> "BLOB";
    };
  }

  @Override
  public String zero(Column column) {
    return switch (column.type()) {
      case BOOLEAN, INT, BIGINT, REAL, DOUBLE -> "0";
      case TEXT -> "''";
      case BYTES -> "X''";
      case UUID -> "'00000000-0000-0000-0000-000000000000'";
      case TIMESTAMP -> "'1970-01-01T00:00:00Z'";
      case JSON -> "'{}'";
    };
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }
}
