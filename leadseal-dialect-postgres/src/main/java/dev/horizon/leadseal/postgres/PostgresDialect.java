package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.BooleanType;
import dev.horizon.leadseal.BytesType;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DateType;
import dev.horizon.leadseal.DecimalType;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.FloatType;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.JsonType;
import dev.horizon.leadseal.SchemaException;
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
 * The DDL of PostgreSQL 13 and later. PostgreSQL writes every command as standard SQL, so this dialect only maps types
 * and values.
 */
public final class PostgresDialect implements Dialect {

  public static final String NAME = "postgresql";

  private static final int MAX_PRECISION = 1000;

  private final CommandFactory commands = new CommandFactory();

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
      case IntegerType type -> type.size() == IntegerType.Size.INT32 ? "integer" : "bigint";
      case FloatType type -> type.size() == FloatType.Size.FLOAT32 ? "real" : "double precision";
      case DecimalType type -> decimal(table, column, type);
      case TextType type -> type.length() == null ? "text" : "varchar(" + type.length() + ")";
      case BytesType _ -> "bytea";
      case UuidType _ -> "uuid";
      case TimestampType _ -> "timestamptz";
      case DateType _ -> "date";
      case TimeType _ -> "time";
      case JsonType _ -> "jsonb";
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
    };
  }

  @Override
  public String generated(Column column, DefaultAttribute value) {
    return switch (value) {
      case NowDefaultAttribute _ -> column.type() instanceof DateType
          ? "((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)"
          : "CURRENT_TIMESTAMP";
      case RandomUuidDefaultAttribute _ -> "gen_random_uuid()";
      default -> throw new IllegalArgumentException("A literal default is not generated.");
    };
  }

  @Override
  public CommandFactory commands() {
    return commands;
  }

  private static String decimal(Table table, Column column, DecimalType type) {
    if (type.precision() > MAX_PRECISION) {
      throw new SchemaException("PostgreSQL cannot store the column " + table.name() + "." + column.name()
          + " with a precision of " + type.precision() + ". The maximum is " + MAX_PRECISION + ".");
    }
    return "numeric(" + type.precision() + ", " + type.scale() + ")";
  }
}
