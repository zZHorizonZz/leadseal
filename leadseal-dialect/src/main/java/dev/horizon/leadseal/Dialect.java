package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefaultAttribute;
import dev.horizon.leadseal.command.CommandFactory;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The DDL of one database: how it quotes identifiers and values, the database type of each column type, and the
 * {@link CommandFactory} that creates its commands. Dialects are services: each dialect module registers its dialect
 * for {@link ServiceLoader}.
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
   * Returns the SQL literal of a value.
   *
   * @param type the type of the value
   * @param value the value, in the canonical text form of the type
   * @return the literal, for example {@code false} or {@code '2026-10-14'}
   */
  String literal(Type type, String value);

  /**
   * Returns the SQL expression of a default that the database generates.
   *
   * @param column the column
   * @param value a {@link NowDefaultAttribute} or a {@link RandomUuidDefaultAttribute}
   * @return the expression, for example {@code CURRENT_TIMESTAMP}
   */
  String generated(Column column, DefaultAttribute value);

  /**
   * Returns the factory of the commands of this database.
   *
   * @return the factory
   */
  CommandFactory commands();

  /**
   * Returns the quoted name of a table with its namespace.
   *
   * @param table the table name
   * @return the quoted namespace, a dot and the quoted name, or only the quoted name for the default namespace
   */
  default String qualified(TableName table) {
    return (table.namespace() == null ? "" : quote(table.namespace()) + ".") + quote(table.name());
  }

  /**
   * Returns the SQL of a default.
   *
   * @param column the column
   * @param value the default
   * @return the literal or the expression
   */
  default String defaultValue(Column column, DefaultAttribute value) {
    return value instanceof LiteralDefaultAttribute literal ? literal(column.type(), literal.value())
        : generated(column, value);
  }

  /**
   * Returns the SQL literal of the zero value of a type.
   *
   * @param type the type
   * @return the literal, for example {@code 0} or {@code ''}
   */
  default String zero(Type type) {
    return literal(type, type.zero());
  }

  /**
   * Returns the value that a migration writes into rows with {@code NULL} when a column stops accepting it: the
   * default of the column, or the zero value of its type.
   *
   * @param column the column
   * @return the literal or the expression
   */
  default String fill(Column column) {
    return column.attribute(DefaultAttribute.class).map(value -> defaultValue(column, value))
        .orElseGet(() -> zero(column.type()));
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
