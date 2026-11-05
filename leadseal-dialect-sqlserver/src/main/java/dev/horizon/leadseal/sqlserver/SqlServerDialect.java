package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Table;
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
import java.util.Optional;

/**
 * The DDL of SQL Server 2017 and later.
 *
 * <p>
 * Text and binary columns without a length are {@code NVARCHAR(MAX)} and {@code VARBINARY(MAX)}. SQL Server cannot
 * index them, so a text or binary column in a key or an index needs a length. Renames use {@code sp_rename}, defaults
 * are named constraints, and comments are {@code MS_Description} extended properties.
 */
public final class SqlServerDialect implements Dialect {

  public static final String NAME = "sqlserver";

  private static final int MAX_NVARCHAR = 4000;
  private static final int ENUM_LENGTH = 63;
  private static final int MAX_VARBINARY = 8000;
  private static final DecimalLimit DECIMAL = new DecimalLimit("SQL Server", 38, 38);
  private static final KeyLimit NVARCHAR = new KeyLimit("SQL Server", "NVARCHAR(MAX)", "Give the column a"
      + " length of at most " + MAX_NVARCHAR + ".");
  private static final KeyLimit VARBINARY = new KeyLimit("SQL Server", "VARBINARY(MAX)", "Give the column a"
      + " length of at most " + MAX_VARBINARY + ".");
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
      .withZone(ZoneOffset.UTC);

  private final Ddl ddl = new SqlServerDdl(this);

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String identifier) {
    return "[" + identifier.replace("]", "]]") + "]";
  }

  @Override
  public String string(String text) {
    return "N'" + text.replace("'", "''") + "'";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case BooleanType _ -> "BIT";
      case IntegerType(IntegerType.Size size) -> size.select("INT", "BIGINT");
      case FloatType(FloatType.Size size) -> size.select("REAL", "FLOAT");
      case DecimalType type -> DECIMAL.sql("DECIMAL", table, column, type);
      case TextType(Length length) -> length.map(characters -> characters <= MAX_NVARCHAR ? "NVARCHAR(" + characters
          + ")" : NVARCHAR.sql(table, column),
          () -> NVARCHAR.sql(table, column));
      case BytesType(Length length) -> length.map(bytes -> bytes <= MAX_VARBINARY ? "VARBINARY(" + bytes + ")"
          : VARBINARY.sql(table, column),
          () -> VARBINARY.sql(table, column));
      case UuidType _ -> "UNIQUEIDENTIFIER";
      case TimestampType _ -> "DATETIME2(6)";
      case DateType _ -> "DATE";
      case TimeType _ -> "TIME(6)";
      case JsonType _ -> NVARCHAR.sql(table, column);
      case EnumType(_, _, EnumType.Storage storage) -> storage.select("NVARCHAR(" + ENUM_LENGTH + ")", "INT");
    };
  }

  @Override
  public String literal(Type type, String value) {
    return switch (type) {
      case BooleanType _ -> value.equals("true") ? "1" : "0";
      case IntegerType _ -> value;
      case FloatType _ -> value;
      case DecimalType _ -> value;
      case BytesType _ -> "0x" + value;
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
    return "CAST(SYSUTCDATETIME() AS DATE)";
  }

  @Override
  public String currentTimestamp() {
    return "SYSUTCDATETIME()";
  }

  @Override
  public String randomUuid() {
    return "NEWID()";
  }

  @Override
  public Ddl ddl() {
    return ddl;
  }

  /**
   * Returns {@code GO}. Flyway sends a SQL Server migration file as one batch up to each {@code GO}, and statements
   * such as {@code CREATE TRIGGER} must be the only statement of their batch.
   */
  @Override
  public Optional<String> batchSeparator() {
    return Optional.of("GO");
  }

  /** SQL Server has no boolean values, so a {@code BIT} value is compared with 1 where a condition belongs. */
  @Override
  public String condition(String value) {
    return "(" + value + " = 1)";
  }

  /** SQL Server has no boolean values, so a condition is {@code 1} or {@code 0} where a value belongs. */
  @Override
  public String value(String condition) {
    return "CASE WHEN " + condition + " THEN 1 ELSE 0 END";
  }

  /**
   * SQL Server joins texts with {@code +}, which gives {@code NULL} if either text is, as {@code ||} does elsewhere.
   * {@code CONCAT} would read {@code NULL} as an empty text.
   */
  @Override
  public String concatenation(String left, String right) {
    return "(" + left + " + " + right + ")";
  }

  /**
   * SQL Server's {@code LEN} does not count the spaces at the end of a text, so the length counts one more character
   * at the end and subtracts it.
   */
  @Override
  public String length(String text) {
    return "(LEN(" + text + " + " + string("x") + ") - 1)";
  }

  /** SQL Server has no comparison where {@code NULL} equals itself, so the difference tests the nulls on its own. */
  @Override
  public String difference(String old, String current) {
    return "((" + old + " <> " + current + ") OR (" + old + " IS NULL AND " + current + " IS NOT NULL) OR (" + old
        + " IS NOT NULL AND " + current + " IS NULL))";
  }

}
