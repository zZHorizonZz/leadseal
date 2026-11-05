package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalLimit;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.Length;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.util.Optional;

/**
 * The DDL of PostgreSQL 13 and later.
 */
public final class PostgresDialect implements Dialect {

  public static final String NAME = "postgresql";

  private static final DecimalLimit DECIMAL = new DecimalLimit("PostgreSQL", 1000, 1000);

  private final Ddl ddl = new PostgresDdl(this);

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
      case BooleanType _ -> "boolean";
      case IntegerType(IntegerType.Size size) -> size.select("integer", "bigint");
      case FloatType(FloatType.Size size) -> size.select("real", "double precision");
      case DecimalType type -> DECIMAL.sql("numeric", table, column, type);
      case TextType(Length length) -> length.map(characters -> "varchar(" + characters + ")", () -> "text");
      case BytesType _ -> "bytea";
      case UuidType _ -> "uuid";
      case TimestampType _ -> "timestamptz";
      case DateType _ -> "date";
      case TimeType _ -> "time";
      case JsonType _ -> "jsonb";
      case EnumType(String name, _, EnumType.Storage storage) -> storage.select(quote(name), "integer");
    };
  }

  @Override
  public String literal(Type type, String value) {
    return switch (type) {
      case BooleanType _ -> value;
      case IntegerType _ -> value;
      case FloatType _ -> value;
      case DecimalType _ -> value;
      case BytesType _ -> string("\\x" + value);
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
    return "((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)";
  }

  @Override
  public String currentTimestamp() {
    return "CURRENT_TIMESTAMP";
  }

  @Override
  public String randomUuid() {
    return "gen_random_uuid()";
  }

  @Override
  public Ddl ddl() {
    return ddl;
  }

  /**
   * Returns a body in dollar quotes, such as the body of a plpgsql function: {@code $leadseal$ ... $leadseal$}. The
   * tag is the first of {@code leadseal}, {@code leadseal1}, {@code leadseal2} and so on that the body does not
   * contain, so the body needs no escaping. Flyway keeps the semicolons inside dollar quotes in one statement.
   *
   * @param body the body, for example {@code BEGIN NEW.total := 0; RETURN NEW; END;}
   * @return the quoted body
   */
  public String dollarQuote(String body) {
    String tag = "$leadseal$";
    for (int i = 1; body.contains(tag); i++) {
      tag = "$leadseal" + i + "$";
    }
    return tag + "\n" + body + "\n" + tag;
  }

  @Override
  public Optional<Check> typeCheck(String table, String column, Type type) {
    return type instanceof EnumType(_, _, EnumType.Storage storage)
        ? storage.select(Optional.empty(), Dialect.super.typeCheck(table, column, type))
        : Optional.empty();
  }
}
