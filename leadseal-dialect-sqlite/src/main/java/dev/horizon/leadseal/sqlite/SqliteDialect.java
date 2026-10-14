package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Change;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnDefinition;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.regex.Pattern;

/**
 * The DDL of SQLite 3.35 and later.
 *
 * <p>
 * SQLite cannot change a column, a primary key, a foreign key or a check of an existing table. A migration with such a
 * change rebuilds the table: it creates the new table, copies the rows, drops the old table and renames the new one,
 * as the SQLite documentation describes. Column lengths are not enforced. SQLite has no database schemas and no
 * comments. An {@code INT} or {@code BIGINT} primary key of one column is the rowid, so SQLite generates its values
 * with or without identity.
 */
public final class SqliteDialect implements Dialect {

  public static final String NAME = "sqlite";

  private static final Pattern COLLATION = Pattern.compile("[A-Za-z0-9_]+");
  private static final String RANDOM_UUID = "(lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4'"
      + " || substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (abs(random()) % 4), 1)"
      + " || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))))";

  private final Decorator decorator = Decorator.of(Change.class, SqliteChange::new)
      .then(Decorator.of(ColumnDefinition.class, definition -> sql -> definition(sql, definition)));

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
      case BOOLEAN, INT, BIGINT -> "INTEGER";
      case REAL, DOUBLE -> "REAL";
      case DECIMAL, TEXT, UUID, TIMESTAMP, DATE, TIME, JSON -> "TEXT";
      case BYTES -> "BLOB";
    };
  }

  @Override
  public String literal(Column column, String value) {
    return switch (column.type()) {
      case BOOLEAN -> value.equals("true") ? "1" : "0";
      case INT, BIGINT, REAL, DOUBLE -> value;
      case BYTES -> "X'" + value + "'";
      case DECIMAL, TEXT, UUID, TIMESTAMP, DATE, TIME, JSON -> string(value);
    };
  }

  @Override
  public String generated(Column column, Default.Kind kind) {
    return switch (kind) {
      case NOW -> column.type() == ColumnType.DATE ? "(date('now'))" : "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))";
      case RANDOM_UUID -> RANDOM_UUID;
      case VALUE -> throw new IllegalArgumentException("A fixed default is a literal.");
    };
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }

  /** Renders a column definition: the name, the type, the collation, {@code NOT NULL} and the default. */
  private static void definition(DdlWriter sql, ColumnDefinition definition) {
    Column column = definition.column();
    sql.name(column.name()).append(" " + sql.dialect().type(definition.table(), column));
    String collation = column.collations().get(NAME);
    if (collation != null) {
      if (!COLLATION.matcher(collation).matches()) {
        throw new SchemaException("The SQLite collation '" + collation + "' is not a collation name.");
      }
      sql.append(" COLLATE " + collation);
    }
    if (!column.nullable()) {
      sql.append(" NOT NULL");
    }
    if (column.defaultValue() != null) {
      sql.append(" DEFAULT " + sql.dialect().defaultValue(column));
    }
  }
}
