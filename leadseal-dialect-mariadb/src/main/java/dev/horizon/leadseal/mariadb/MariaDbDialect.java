package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnComment;
import dev.horizon.leadseal.ColumnDefinition;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The DDL of MariaDB 11.4 and later.
 *
 * <p>
 * Text and binary columns without a length are {@code LONGTEXT} and {@code LONGBLOB}. MariaDB cannot index them, so a
 * text or binary column in a key or an index needs a length. A database schema is a MariaDB database. MariaDB has no
 * partial indexes.
 */
public final class MariaDbDialect implements Dialect {

  public static final String NAME = "mariadb";

  private static final int MAX_PRECISION = 65;
  private static final int MAX_SCALE = 38;
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
      .withZone(ZoneOffset.UTC);
  private static final Pattern COLLATION = Pattern.compile("[A-Za-z0-9_]+");

  private final Decorator decorator = Decorator.of(ColumnDefinition.class, MariaDbOperation.Definition::new)
      .then(Decorator.of(ColumnComment.class, comment -> MariaDbOperation.NOTHING))
      .then(Decorator.of(Operation.RenameTable.class, MariaDbOperation.RenameTable::new))
      .then(Decorator.of(Operation.MoveTable.class, MariaDbOperation.MoveTable::new))
      .then(Decorator.of(Operation.AlterColumn.class, MariaDbOperation.AlterColumn::new))
      .then(Decorator.of(Operation.AlterPrimaryKey.class, MariaDbOperation.AlterPrimaryKey::new))
      .then(Decorator.of(Operation.AddIndex.class, MariaDbOperation.AddIndex::new))
      .then(Decorator.of(Operation.DropIndex.class, MariaDbOperation.DropIndex::new))
      .then(Decorator.of(Operation.DropForeignKey.class, MariaDbOperation.DropForeignKey::new))
      .then(Decorator.of(Operation.CommentTable.class, MariaDbOperation.CommentTable::new));

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
      case BOOLEAN -> "BOOLEAN";
      case INT -> "INT";
      case BIGINT -> "BIGINT";
      case REAL -> "FLOAT";
      case DOUBLE -> "DOUBLE";
      case DECIMAL -> decimal(table, column);
      case TEXT -> column.length() != null ? "VARCHAR(" + column.length() + ")" : unbounded(table, column, "LONGTEXT");
      case BYTES -> column.length() != null ? "VARBINARY(" + column.length() + ")"
          : unbounded(table, column, "LONGBLOB");
      case UUID -> "UUID";
      case TIMESTAMP -> "DATETIME(6)";
      case DATE -> "DATE";
      case TIME -> "TIME(6)";
      case JSON -> unbounded(table, column, "JSON");
    };
  }

  @Override
  public String literal(Column column, String value) {
    return switch (column.type()) {
      case BOOLEAN -> value.toUpperCase(Locale.ROOT);
      case INT, BIGINT, REAL, DOUBLE, DECIMAL -> value;
      case BYTES -> "X'" + value + "'";
      case TIMESTAMP -> string(TIMESTAMP.format(Instant.parse(value)));
      case TEXT, UUID, DATE, TIME, JSON -> string(value);
    };
  }

  @Override
  public String generated(Column column, Default.Kind kind) {
    return switch (kind) {
      case NOW -> column.type() == ColumnType.DATE ? "(UTC_DATE())" : "(UTC_TIMESTAMP(6))";
      case RANDOM_UUID -> "(UUID())";
      case VALUE -> throw new IllegalArgumentException("A fixed default is a literal.");
    };
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }

  /**
   * Returns a collation name, which MariaDB writes without quotes.
   *
   * @param collation the collation, for example {@code utf8mb4_bin}
   * @return the collation
   * @throws SchemaException if the name has other characters than letters, digits and underscores
   */
  static String collation(String collation) {
    if (!COLLATION.matcher(collation).matches()) {
      throw new SchemaException("The MariaDB collation '" + collation + "' is not a collation name.");
    }
    return collation;
  }

  private static String decimal(Table table, Column column) {
    int scale = column.scale() == null ? 0 : column.scale();
    if (column.precision() > MAX_PRECISION || scale > MAX_SCALE) {
      throw new SchemaException("MariaDB cannot store the column " + table.name() + "." + column.name() + " with a"
          + " precision of " + column.precision() + " and a scale of " + scale + ". The maximum precision is "
          + MAX_PRECISION + " and the maximum scale is " + MAX_SCALE + ".");
    }
    return "DECIMAL(" + column.precision() + ", " + scale + ")";
  }

  private static String unbounded(Table table, Column column, String type) {
    if (table.keyed(column.name())) {
      throw new SchemaException("MariaDB cannot use the column " + table.name() + "." + column.name() + " in a key"
          + " or an index, because its type is " + type + ". Give the column a length.");
    }
    return type;
  }
}
