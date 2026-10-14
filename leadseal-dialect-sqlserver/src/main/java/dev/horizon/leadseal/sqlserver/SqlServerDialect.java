package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnComment;
import dev.horizon.leadseal.ColumnDefinition;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.CreateSchema;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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
  private static final Pattern COLLATION = Pattern.compile("[A-Za-z0-9_]+");

  private final Decorator decorator = Decorator.of(ColumnDefinition.class, SqlServerOperation.Definition::new)
      .then(Decorator.of(ColumnComment.class, SqlServerOperation.CommentColumn::new))
      .then(Decorator.of(CreateSchema.class, SqlServerOperation.CreateSchemaIfMissing::new))
      .then(Decorator.of(Operation.RenameTable.class, SqlServerOperation.RenameTable::new))
      .then(Decorator.of(Operation.MoveTable.class, SqlServerOperation.MoveTable::new))
      .then(Decorator.of(Operation.RenameColumn.class, SqlServerOperation.RenameColumn::new))
      .then(Decorator.of(Operation.AddColumn.class, SqlServerOperation.AddColumn::new))
      .then(Decorator.of(Operation.DropColumn.class, SqlServerOperation.DropColumn::new))
      .then(Decorator.of(Operation.AlterColumn.class, SqlServerOperation.AlterColumn::new))
      .then(Decorator.of(Operation.DropIndex.class, SqlServerOperation.DropIndex::new))
      .then(Decorator.of(Operation.AddForeignKey.class, SqlServerOperation.AddForeignKey::new))
      .then(Decorator.of(Operation.CommentTable.class, SqlServerOperation.CommentTable::new));

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
      case BOOLEAN -> "BIT";
      case INT -> "INT";
      case BIGINT -> "BIGINT";
      case REAL -> "REAL";
      case DOUBLE -> "FLOAT";
      case DECIMAL -> decimal(table, column);
      case TEXT -> column.length() != null && column.length() <= MAX_NVARCHAR ? "NVARCHAR(" + column.length() + ")"
          : unbounded(table, column, "NVARCHAR(MAX)");
      case BYTES -> column.length() != null && column.length() <= MAX_VARBINARY
          ? "VARBINARY(" + column.length() + ")"
          : unbounded(table, column, "VARBINARY(MAX)");
      case UUID -> "UNIQUEIDENTIFIER";
      case TIMESTAMP -> "DATETIME2(6)";
      case DATE -> "DATE";
      case TIME -> "TIME(6)";
      case JSON -> unbounded(table, column, "NVARCHAR(MAX)");
    };
  }

  @Override
  public String literal(Column column, String value) {
    return switch (column.type()) {
      case BOOLEAN -> value.equals("true") ? "1" : "0";
      case INT, BIGINT, REAL, DOUBLE, DECIMAL -> value;
      case BYTES -> "0x" + value;
      case TIMESTAMP -> string(TIMESTAMP.format(Instant.parse(value)));
      case TEXT, UUID, DATE, TIME, JSON -> string(value);
    };
  }

  @Override
  public String generated(Column column, Default.Kind kind) {
    return switch (kind) {
      case NOW -> column.type() == ColumnType.DATE ? "CAST(SYSUTCDATETIME() AS DATE)" : "SYSUTCDATETIME()";
      case RANDOM_UUID -> "NEWID()";
      case VALUE -> throw new IllegalArgumentException("A fixed default is a literal.");
    };
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }

  /**
   * Returns a collation name, which SQL Server writes without quotes.
   *
   * @param collation the collation, for example {@code Latin1_General_BIN2}
   * @return the collation
   * @throws SchemaException if the name has other characters than letters, digits and underscores
   */
  static String collation(String collation) {
    if (!COLLATION.matcher(collation).matches()) {
      throw new SchemaException("The SQL Server collation '" + collation + "' is not a collation name.");
    }
    return collation;
  }

  private static String decimal(Table table, Column column) {
    if (column.precision() > MAX_PRECISION) {
      throw new SchemaException("SQL Server cannot store the column " + table.name() + "." + column.name() + " with"
          + " a precision of " + column.precision() + ". The maximum is " + MAX_PRECISION + ".");
    }
    return "DECIMAL(" + column.precision() + ", " + (column.scale() == null ? 0 : column.scale()) + ")";
  }

  private static String unbounded(Table table, Column column, String type) {
    if (table.keyed(column.name())) {
      throw new SchemaException("SQL Server cannot use the column " + table.name() + "." + column.name() + " in a"
          + " key or an index, because its type is " + type + ". Give the column a length of at most "
          + (column.type() == ColumnType.BYTES ? MAX_VARBINARY : MAX_NVARCHAR) + ".");
    }
    return type;
  }
}
