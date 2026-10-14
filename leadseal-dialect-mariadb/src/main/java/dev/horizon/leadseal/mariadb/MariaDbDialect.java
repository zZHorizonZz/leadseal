package dev.horizon.leadseal.mariadb;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The DDL of MariaDB 11.4 and later.
 *
 * <p>
 * Text and binary columns without a length are {@code LONGTEXT} and {@code LONGBLOB}. MariaDB cannot index them, so a
 * text or binary column in a key or an index needs a length. A namespace is a MariaDB database. MariaDB has no partial
 * indexes.
 */
public final class MariaDbDialect implements Dialect {

  public static final String NAME = "mariadb";

  private static final int MAX_PRECISION = 65;
  private static final int MAX_SCALE = 38;
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
      .withZone(ZoneOffset.UTC);

  private final CommandFactory commands = new MariaDbCommandFactory();

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String identifier) {
    return "`" + identifier.replace("`", "``") + "`";
  }

  @Override
  public String string(String text) {
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case BooleanType _ -> "BOOLEAN";
      case IntegerType type -> type.size() == IntegerType.Size.INT32 ? "INT" : "BIGINT";
      case FloatType type -> type.size() == FloatType.Size.FLOAT32 ? "FLOAT" : "DOUBLE";
      case DecimalType type -> decimal(table, column, type);
      case TextType type -> type.length() != null ? "VARCHAR(" + type.length() + ")"
          : unbounded(table, column, "LONGTEXT");
      case BytesType type -> type.length() != null ? "VARBINARY(" + type.length() + ")"
          : unbounded(table, column, "LONGBLOB");
      case UuidType _ -> "UUID";
      case TimestampType _ -> "DATETIME(6)";
      case DateType _ -> "DATE";
      case TimeType _ -> "TIME(6)";
      case JsonType _ -> unbounded(table, column, "JSON");
    };
  }

  @Override
  public String literal(Type type, String value) {
    return switch (type) {
      case BooleanType _ -> value.toUpperCase(Locale.ROOT);
      case IntegerType _ -> value;
      case FloatType _ -> value;
      case DecimalType _ -> value;
      case BytesType _ -> "X'" + value + "'";
      case TimestampType _ -> string(TIMESTAMP.format(Instant.parse(value)));
      case TextType _ -> string(value);
      case UuidType _ -> string(value);
      case DateType _ -> string(value);
      case TimeType _ -> string(value);
      case JsonType _ -> string(value);
    };
  }

  @Override
  public String generated(Column column, DefaultAttribute value) {
    return switch (value) {
      case NowDefaultAttribute _ -> column.type() instanceof DateType ? "(UTC_DATE())" : "(UTC_TIMESTAMP(6))";
      case RandomUuidDefaultAttribute _ -> "(UUID())";
      default -> throw new IllegalArgumentException("A literal default is not generated.");
    };
  }

  @Override
  public CommandFactory commands() {
    return commands;
  }

  private static String decimal(Table table, Column column, DecimalType type) {
    if (type.precision() > MAX_PRECISION || type.scale() > MAX_SCALE) {
      throw new SchemaException("MariaDB cannot store the column " + table.name() + "." + column.name() + " with a"
          + " precision of " + type.precision() + " and a scale of " + type.scale() + ". The maximum precision is "
          + MAX_PRECISION + " and the maximum scale is " + MAX_SCALE + ".");
    }
    return "DECIMAL(" + type.precision() + ", " + type.scale() + ")";
  }

  private static String unbounded(Table table, Column column, String type) {
    if (table.keyed(column.name())) {
      throw new SchemaException("MariaDB cannot use the column " + table.name() + "." + column.name() + " in a key"
          + " or an index, because its type is " + type + ". Give the column a length.");
    }
    return type;
  }
}
