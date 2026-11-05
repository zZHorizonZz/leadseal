package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ConstraintName;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.In;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.SqlEnvironment;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.Type;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * The DDL of one database: how it quotes identifiers and values, the database type of each column type, and the
 * {@link Ddl} of its statements. Dialects are services: each dialect module registers its dialect
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
   * Gives the check that a column of an enum type needs, because the database type of the column holds more than the
   * values of the enum. A dialect with a type that holds only the values overrides this for the enums that it writes
   * with that type.
   *
   * @param table the name of the table
   * @param column the name of the column
   * @param type the type of the column
   * @return the check {@code ck_<table>_<column>}, or empty for a column that needs none. The default checks every enum
   * column.
   */
  default Optional<Check> typeCheck(String table, String column, Type type) {
    if (!(type instanceof EnumType values)) {
      return Optional.empty();
    }
    return Optional.of(new Check(new ConstraintName("ck", table, List.of(column)).value(),
        new In(ColumnReference.of(column), values.literals().stream()
            .<SqlExpression> map(value -> new Literal(values, value)).toList()).condition(SqlEnvironment.of(this))));
  }

  /**
   * Returns the SQL expression of the current date in UTC.
   *
   * @return the expression, for example {@code CURRENT_DATE}
   */
  String currentDate();

  /**
   * Returns the SQL expression of the current timestamp.
   *
   * @return the expression, for example {@code CURRENT_TIMESTAMP}
   */
  String currentTimestamp();

  /**
   * Returns the SQL expression of a random UUID.
   *
   * @return the expression, for example {@code gen_random_uuid()}
   */
  String randomUuid();

  /**
   * Returns the statements of this database for the changes of a migration.
   *
   * @return the statements
   */
  Ddl ddl();

  /**
   * Returns the line that ends a batch of statements in a migration file. A database that runs a file as batches needs
   * it around a statement that must be the only statement of its batch, such as {@code CREATE TRIGGER} on SQL Server.
   *
   * @return the separator, for example {@code GO}, or empty if the database runs each statement of a file on its own.
   * The default is empty.
   * @see ScriptWriter#isolate()
   */
  default Optional<String> batchSeparator() {
    return Optional.empty();
  }

  /**
   * Returns the SQL of a value where SQL expects a condition, as after {@code WHERE}.
   *
   * @param value the SQL of a boolean value, for example a column
   * @return the condition. The default is the value itself.
   */
  default String condition(String value) {
    return value;
  }

  /**
   * Returns the SQL of a condition where SQL expects a value, as a computed column.
   *
   * @param condition the SQL of a condition, for example a comparison
   * @return the boolean value. The default is the condition itself.
   */
  default String value(String condition) {
    return condition;
  }

  /**
   * Returns the SQL of two texts, one after the other. The value is {@code NULL} if either text is.
   *
   * @param left the SQL of the first text
   * @param right the SQL of the second text
   * @return the SQL. The default is {@code (left || right)}.
   */
  default String concatenation(String left, String right) {
    return "(" + left + " || " + right + ")";
  }

  /**
   * Returns the SQL of the quotient of two integers, rounded toward zero.
   *
   * @param dividend the SQL of the dividend
   * @param divisor the SQL of the divisor
   * @return the SQL. The default is {@code (dividend / divisor)}.
   */
  default String quotient(String dividend, String divisor) {
    return "(" + dividend + " / " + divisor + ")";
  }

  /**
   * Returns the SQL of the number of characters of a text, with the spaces at its end.
   *
   * @param text the SQL of the text
   * @return the SQL. The default is {@code CHAR_LENGTH(text)}.
   */
  default String length(String text) {
    return "CHAR_LENGTH(" + text + ")";
  }

  /**
   * Returns the SQL of whether two values differ, where {@code NULL} equals itself, as an update changes a column.
   *
   * @param old the SQL of the old value
   * @param current the SQL of the new value
   * @return the condition. The default is {@code (old IS DISTINCT FROM current)}.
   */
  default String difference(String old, String current) {
    return "(" + old + " IS DISTINCT FROM " + current + ")";
  }

  /**
   * Returns the quoted name of a table with its namespace.
   *
   * @param table the table name
   * @return the quoted namespace, a dot and the quoted name, or only the quoted name for the default namespace
   */
  default String quote(TableName table) {
    return table.namespace().map(namespace -> quote(namespace) + ".", () -> "") + quote(table.name());
  }

  /**
   * Returns the SQL of a default.
   *
   * @param column the column
   * @param value the default
   * @return the literal or the expression
   */
  default String defaultValue(Column column, Default value) {
    return value.sql(this, column);
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
    return column.attribute(Default.class).map(value -> defaultValue(column, value))
        .orElseGet(() -> zero(column.type()));
  }

  /**
   * Finds an installed dialect by name. Dialect modules register their dialect for {@link ServiceLoader}.
   *
   * @param name the dialect name, for example {@code postgresql}
   * @return the dialect
   * @throws SchemaException if no installed dialect has that name
   */
  static Dialect of(String name) {
    List<Dialect> dialects = ServiceLoader.load(Dialect.class).stream().map(ServiceLoader.Provider::get).toList();
    return dialects.stream().filter(dialect -> dialect.name().equals(name)).findFirst()
        .orElseThrow(() -> new ConfigurationException("No installed dialect is named '" + name + "'. Installed dialects: "
            + dialects.stream().map(Dialect::name).sorted().toList() + "."));
  }
}
