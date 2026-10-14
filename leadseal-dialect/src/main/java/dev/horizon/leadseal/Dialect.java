package dev.horizon.leadseal;

import java.util.List;
import java.util.ServiceLoader;

/**
 * The DDL of one database.
 *
 * <p>
 * A dialect defines how to quote identifiers, the database type of each column type, and a {@link Decorator} that
 * rewrites the operations that the database writes differently from standard SQL. Dialects are services: each dialect
 * module registers its dialect for {@link ServiceLoader}.
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
   * Returns the database type of a column.
   *
   * @param table the table of the column
   * @param column the column
   * @return the database type, for example {@code varchar(255)}
   * @throws SchemaException if the database cannot store the column, for example an unbounded text column in an index
   */
  String type(Table table, Column column);

  /**
   * Returns the SQL literal of the default value of a column type: {@code 0}, {@code false}, an empty text or empty
   * bytes. Migrations fill new and existing rows with it when a column becomes {@code NOT NULL}.
   *
   * @param column the column
   * @return the literal
   */
  String zero(Column column);

  /**
   * Returns the decorator that rewrites operations into the DDL of this database.
   *
   * @return the decorator
   */
  Decorator decorator();

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
