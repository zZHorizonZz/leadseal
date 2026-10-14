package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.BooleanType;
import dev.horizon.leadseal.BytesType;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DateType;
import dev.horizon.leadseal.DecimalType;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.FloatType;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.JsonType;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TextType;
import dev.horizon.leadseal.TimeType;
import dev.horizon.leadseal.TimestampType;
import dev.horizon.leadseal.Type;
import dev.horizon.leadseal.UuidType;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefaultAttribute;
import dev.horizon.leadseal.command.CommandFactory;

/**
 * The DDL of SQLite 3.35 and later.
 *
 * <p>
 * SQLite cannot change a column, a primary key, a foreign key or a check of an existing table. A migration with such a
 * change rebuilds the table: it creates the new table, copies the rows, drops the old table and renames the new one,
 * as the SQLite documentation describes. Column lengths are not enforced. SQLite has no namespaces and no comments. An
 * integer primary key of one column is the rowid, so SQLite generates its values with or without identity.
 */
public final class SqliteDialect implements Dialect {

  public static final String NAME = "sqlite";

  private static final String RANDOM_UUID = "(lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4'"
      + " || substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (abs(random()) % 4), 1)"
      + " || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))))";

  private final CommandFactory commands = new SqliteCommandFactory();

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
      case BooleanType _ -> "INTEGER";
      case IntegerType _ -> "INTEGER";
      case FloatType _ -> "REAL";
      case DecimalType _ -> "TEXT";
      case TextType _ -> "TEXT";
      case BytesType _ -> "BLOB";
      case UuidType _ -> "TEXT";
      case TimestampType _ -> "TEXT";
      case DateType _ -> "TEXT";
      case TimeType _ -> "TEXT";
      case JsonType _ -> "TEXT";
    };
  }

  @Override
  public String literal(Type type, String value) {
    return switch (type) {
      case BooleanType _ -> value.equals("true") ? "1" : "0";
      case IntegerType _ -> value;
      case FloatType _ -> value;
      case BytesType _ -> "X'" + value + "'";
      case DecimalType _ -> string(value);
      case TextType _ -> string(value);
      case UuidType _ -> string(value);
      case TimestampType _ -> string(value);
      case DateType _ -> string(value);
      case TimeType _ -> string(value);
      case JsonType _ -> string(value);
    };
  }

  @Override
  public String generated(Column column, DefaultAttribute value) {
    return switch (value) {
      case NowDefaultAttribute _ -> column.type() instanceof DateType ? "(date('now'))"
          : "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))";
      case RandomUuidDefaultAttribute _ -> RANDOM_UUID;
      default -> throw new IllegalArgumentException("A literal default is not generated.");
    };
  }

  @Override
  public CommandFactory commands() {
    return commands;
  }
}
