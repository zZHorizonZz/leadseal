package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Decorator;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;

/**
 * The DDL of MariaDB 11.4 and later.
 *
 * <p>
 * Text and binary columns without a length are {@code LONGTEXT} and {@code LONGBLOB}. MariaDB cannot index them, so a
 * text or binary column in a key or an index needs a length.
 */
public final class MariaDbDialect implements Dialect {

  public static final String NAME = "mariadb";

  private final Decorator decorator = Decorator.of(Operation.RenameTable.class, MariaDbOperation.RenameTable::new)
      .then(Decorator.of(Operation.AlterColumn.class, MariaDbOperation.AlterColumn::new))
      .then(Decorator.of(Operation.AlterPrimaryKey.class, MariaDbOperation.AlterPrimaryKey::new))
      .then(Decorator.of(Operation.DropIndex.class, MariaDbOperation.DropIndex::new))
      .then(Decorator.of(Operation.DropForeignKey.class, MariaDbOperation.DropForeignKey::new));

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String quote(String identifier) {
    return "`" + identifier.replace("`", "``") + "`";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case BOOLEAN -> "BOOLEAN";
      case INT -> "INT";
      case BIGINT -> "BIGINT";
      case REAL -> "FLOAT";
      case DOUBLE -> "DOUBLE";
      case TEXT -> column.length() != null ? "VARCHAR(" + column.length() + ")" : unbounded(table, column, "LONGTEXT");
      case BYTES -> column.length() != null ? "VARBINARY(" + column.length() + ")"
          : unbounded(table, column, "LONGBLOB");
      case UUID -> "UUID";
      case TIMESTAMP -> "DATETIME(6)";
      case JSON -> unbounded(table, column, "JSON");
    };
  }

  @Override
  public String zero(Column column) {
    return switch (column.type()) {
      case BOOLEAN -> "FALSE";
      case INT, BIGINT, REAL, DOUBLE -> "0";
      case TEXT, BYTES -> "''";
      case UUID -> "'00000000-0000-0000-0000-000000000000'";
      case TIMESTAMP -> "'1970-01-01 00:00:00'";
      case JSON -> "'{}'";
    };
  }

  @Override
  public Decorator decorator() {
    return decorator;
  }

  private static String unbounded(Table table, Column column, String type) {
    if (table.keyed(column.name())) {
      throw new SchemaException("MariaDB cannot use the column " + table.name() + "." + column.name() + " in a key"
          + " or an index, because its type is " + type + ". Give the column a length.");
    }
    return type;
  }
}
