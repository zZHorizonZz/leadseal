package dev.horizon.leadseal.mariadb;

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
import dev.horizon.leadseal.type.KeyLimit;
import dev.horizon.leadseal.type.Length;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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

  private static final DecimalLimit DECIMAL = new DecimalLimit("MariaDB", 65, 38);
  private static final KeyLimit LONGTEXT = new KeyLimit("MariaDB", "LONGTEXT", "Give the column a length.");
  private static final KeyLimit LONGBLOB = new KeyLimit("MariaDB", "LONGBLOB", "Give the column a length.");
  private static final KeyLimit JSON = new KeyLimit("MariaDB", "JSON", "Use a text column instead.");
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
      .withZone(ZoneOffset.UTC);

  private final Ddl ddl = new MariaDbDdl(this);

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
      case IntegerType(IntegerType.Size size) -> size.select("INT", "BIGINT");
      case FloatType(FloatType.Size size) -> size.select("FLOAT", "DOUBLE");
      case DecimalType type -> DECIMAL.sql("DECIMAL", table, column, type);
      case TextType(Length length) -> length.map(characters -> "VARCHAR(" + characters + ")",
          () -> LONGTEXT.sql(table, column));
      case BytesType(Length length) -> length.map(bytes -> "VARBINARY(" + bytes + ")",
          () -> LONGBLOB.sql(table, column));
      case UuidType _ -> "UUID";
      case TimestampType _ -> "DATETIME(6)";
      case DateType _ -> "DATE";
      case TimeType _ -> "TIME(6)";
      case JsonType _ -> JSON.sql(table, column);
      case EnumType type -> type.storage().select("ENUM(" + type.literals().stream().map(this::string)
          .collect(Collectors.joining(", ")) + ")", "INT");
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
      case EnumType(_, _, EnumType.Storage storage) -> storage.select(string(value), value);
    };
  }

  @Override
  public String currentDate() {
    return "(UTC_DATE())";
  }

  @Override
  public String currentTimestamp() {
    return "(UTC_TIMESTAMP(6))";
  }

  @Override
  public String randomUuid() {
    return "(UUID())";
  }

  @Override
  public Ddl ddl() {
    return ddl;
  }

  /** MariaDB reads {@code ||} as {@code OR}, so texts join with {@code CONCAT}. */
  @Override
  public String concatenation(String left, String right) {
    return "CONCAT(" + left + ", " + right + ")";
  }

  /** MariaDB gives a decimal for {@code /} of two integers, so they divide with {@code DIV}. */
  @Override
  public String quotient(String dividend, String divisor) {
    return "(" + dividend + " DIV " + divisor + ")";
  }

  /** MariaDB compares values where {@code NULL} equals itself with {@code <=>}. */
  @Override
  public String difference(String old, String current) {
    return "(NOT (" + old + " <=> " + current + "))";
  }

  @Override
  public Optional<Check> typeCheck(String table, String column, Type type) {
    return type instanceof EnumType(_, _, EnumType.Storage storage)
        ? storage.select(Optional.empty(), Dialect.super.typeCheck(table, column, type))
        : Optional.empty();
  }
}
