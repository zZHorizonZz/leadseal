package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;

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

  private final Ddl ddl = new SqliteDdl(this);

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
      case EnumType(_, _, EnumType.Storage storage) -> storage.select("TEXT", "INTEGER");
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
      case EnumType(_, _, EnumType.Storage storage) -> storage.select(string(value), value);
    };
  }

  @Override
  public String currentDate() {
    return "(date('now'))";
  }

  @Override
  public String currentTimestamp() {
    return "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))";
  }

  @Override
  public String randomUuid() {
    return RANDOM_UUID;
  }

  @Override
  public Ddl ddl() {
    return ddl;
  }

  /** SQLite counts the characters of a text with {@code length}. */
  @Override
  public String length(String text) {
    return "length(" + text + ")";
  }

  /** SQLite compares values where {@code NULL} equals itself with {@code IS NOT}. */
  @Override
  public String difference(String old, String current) {
    return "(" + old + " IS NOT " + current + ")";
  }

}
