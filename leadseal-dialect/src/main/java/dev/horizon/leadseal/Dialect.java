package dev.horizon.leadseal;

import java.util.List;
import java.util.ServiceLoader;

/**
 * The DDL of one database.
 *
 * <p>
 * A dialect defines how to quote identifiers and values, the database type of each column type, and a
 * {@link Decorator} that rewrites the nodes that the database writes differently from standard SQL. Dialects are
 * services: each dialect module registers its dialect for {@link ServiceLoader}.
 */
public interface Dialect {

  /**
   * Returns the dialect name. Migration files of this dialect are in a folder with this name.
   *
   * @return the name, for example {@code postgresql}
   */
  String name();

  /**
   * Quotes an identifier.
   *
   * @param identifier the identifier, such as a table name or a column name
   * @return the quoted identifier
   */
  String quote(String identifier);

  /**
   * Returns a SQL string literal.
   *
   * @param text the text
   * @return the literal, for example {@code 'it''s'}
   */
  String string(String text);

  /**
   * Returns the database type of a column.
   *
   * @param table the table of the column
   * @param column the column
   * @return the database type, for example {@code varchar(255)}
   * @throws SchemaException if the database cannot store the column, for example an unbounded text column in an index
   */
  String type(Table table, Column column);

  /**
   * Returns the SQL literal of a value of a column.
   *
   * @param column the column
   * @param value the value, in the canonical text form of the column type
   * @return the literal, for example {@code false} or {@code '2026-10-14'}
   */
  String literal(Column column, String value);

  /**
   * Returns the SQL expression of a generated default.
   *
   * @param column the column
   * @param kind {@link Default.Kind#NOW} or {@link Default.Kind#RANDOM_UUID}
   * @return the expression, for example {@code CURRENT_TIMESTAMP}
   */
  String generated(Column column, Default.Kind kind);

  /**
   * Returns the decorator that rewrites nodes into the DDL of this database.
   *
   * @return the decorator
   */
  Decorator decorator();

  /**
   * Returns the SQL of the default of a column.
   *
   * @param column a column with a default
   * @return the literal or the expression
   */
  default String defaultValue(Column column) {
    Default value = column.defaultValue();
    return value.kind() == Default.Kind.VALUE ? literal(column, value.value()) : generated(column, value.kind());
  }

  /**
   * Returns the SQL literal of the zero value of a column type: {@code 0}, {@code false}, an empty text and so on.
   *
   * @param column the column
   * @return the literal
   */
  default String zero(Column column) {
    return literal(column, column.type().zero());
  }

  /**
   * Returns the value that a migration writes into existing rows when a column stops accepting {@code NULL}: the
   * default of the column, or its zero value.
   *
   * @param column the column
   * @return the literal or the expression
   */
  default String fill(Column column) {
    return column.defaultValue() == null ? zero(column) : defaultValue(column);
  }

  /**
   * Finds an installed dialect by name. Dialect modules register their dialect for {@link ServiceLoader}.
   *
   * @param name the dialect name, for example {@code postgresql}
   * @return the dialect
   * @throws SchemaException if no installed dialect has that name
   */
  static Dialect named(String name) {
    List<Dialect> installed = ServiceLoader.load(Dialect.class).stream().map(ServiceLoader.Provider::get).toList();
    return installed.stream().filter(dialect -> dialect.name().equals(name)).findFirst()
        .orElseThrow(() -> new SchemaException("No installed dialect is named '" + name + "'. Installed dialects: "
            + installed.stream().map(Dialect::name).sorted().toList() + "."));
  }
}
