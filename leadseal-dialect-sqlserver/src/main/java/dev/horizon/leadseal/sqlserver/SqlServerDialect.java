package dev.horizon.leadseal.sqlserver;

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
  private static final int MAX_VARBINARY = 8000;
  private static final int MAX_PRECISION = 38;
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
      .withZone(ZoneOffset.UTC);

  private final CommandFactory commands = new SqlServerCommandFactory();

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
      case IntegerType type -> type.size() == IntegerType.Size.INT32 ? "INT" : "BIGINT";
      case FloatType type -> type.size() == FloatType.Size.FLOAT32 ? "REAL" : "FLOAT";
      case DecimalType type -> decimal(table, column, type);
      case TextType type -> type.length() != null && type.length() <= MAX_NVARCHAR ? "NVARCHAR(" + type.length() + ")"
          : unbounded(table, column, "NVARCHAR(MAX)", MAX_NVARCHAR);
      case BytesType type -> type.length() != null && type.length() <= MAX_VARBINARY
          ? "VARBINARY(" + type.length() + ")"
          : unbounded(table, column, "VARBINARY(MAX)", MAX_VARBINARY);
      case UuidType _ -> "UNIQUEIDENTIFIER";
      case TimestampType _ -> "DATETIME2(6)";
      case DateType _ -> "DATE";
      case TimeType _ -> "TIME(6)";
      case JsonType _ -> unbounded(table, column, "NVARCHAR(MAX)", MAX_NVARCHAR);
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
    };
  }

  @Override
  public String generated(Column column, DefaultAttribute value) {
    return switch (value) {
      case NowDefaultAttribute _ -> column.type() instanceof DateType ? "CAST(SYSUTCDATETIME() AS DATE)"
          : "SYSUTCDATETIME()";
      case RandomUuidDefaultAttribute _ -> "NEWID()";
      default -> throw new IllegalArgumentException("A literal default is not generated.");
    };
  }

  @Override
  public CommandFactory commands() {
    return commands;
  }

  private static String decimal(Table table, Column column, DecimalType type) {
    if (type.precision() > MAX_PRECISION) {
      throw new SchemaException("SQL Server cannot store the column " + table.name() + "." + column.name() + " with"
          + " a precision of " + type.precision() + ". The maximum is " + MAX_PRECISION + ".");
    }
    return "DECIMAL(" + type.precision() + ", " + type.scale() + ")";
  }

  private static String unbounded(Table table, Column column, String type, int maximum) {
    if (table.keyed(column.name())) {
      throw new SchemaException("SQL Server cannot use the column " + table.name() + "." + column.name() + " in a"
          + " key or an index, because its type is " + type + ". Give the column a length of at most " + maximum
          + ".");
    }
    return type;
  }
}
