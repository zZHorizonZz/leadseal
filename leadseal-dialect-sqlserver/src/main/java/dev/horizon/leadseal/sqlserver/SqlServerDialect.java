package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;

/**
 * The DDL of SQL Server 2017 and later.
 *
 * <p>
 * Text and binary columns without a length are {@code NVARCHAR(MAX)} and {@code VARBINARY(MAX)}. SQL Server cannot
 * index them, so a text or binary column in a key or an index needs a length. Renames use {@code sp_rename}.
 */
public final class SqlServerDialect implements Dialect {

  public static final String NAME = "sqlserver";

  private static final int MAX_NVARCHAR = 4000;
  private static final int MAX_VARBINARY = 8000;

  private final Decorator decorator = Decorator.of(Operation.RenameTable.class, SqlServerOperation.RenameTable::new)
      .then(Decorator.of(Operation.RenameColumn.class, SqlServerOperation.RenameColumn::new))
      .then(Decorator.of(Operation.AddColumn.class, SqlServerOperation.AddColumn::new))
      .then(Decorator.of(Operation.AlterColumn.class, SqlServerOperation.AlterColumn::new))
      .then(Decorator.of(Operation.DropIndex.class, SqlServerOperation.DropIndex::new))
      .then(Decorator.of(Operation.AddForeignKey.class, SqlServerOperation.AddForeignKey::new));

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String identifier) {
    return "[" + identifier.replace("]", "]]") + "]";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case BOOLEAN -> "BIT";
      case INT -> "INT";
      case BIGINT -> "BIGINT";
      case REAL -> "REAL";
      case DOUBLE -> "FLOAT";
      case TEXT -> column.length() != null && column.length() <= MAX_NVARCHAR ? "NVARCHAR(" + column.length() + ")"
          : unbounded(table, column, "NVARCHAR(MAX)");
      case BYTES -> column.length() != null && column.length() <= MAX_VARBINARY
          ? "VARBINARY(" + column.length() + ")"
          : unbounded(table, column, "VARBINARY(MAX)");
      case UUID -> "UNIQUEIDENTIFIER";
      case TIMESTAMP -> "DATETIME2(6)";
      case JSON -> unbounded(table, column, "NVARCHAR(MAX)");
    };
  }

  @Override
  public String zero(Column column) {
    return switch (column.type()) {
      case BOOLEAN, INT, BIGINT, REAL, DOUBLE -> "0";
      case TEXT -> "N''";
      case BYTES -> "0x";
      case UUID -> "'00000000-0000-0000-0000-000000000000'";
      case TIMESTAMP -> "'1970-01-01T00:00:00'";
      case JSON -> "N'{}'";
    };
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }

  /**
   * Returns a SQL string literal.
   *
   * @param text the text
   * @return the literal, for example {@code N'orders'}
   */
  static String literal(String text) {
    return "N'" + text.replace("'", "''") + "'";
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
