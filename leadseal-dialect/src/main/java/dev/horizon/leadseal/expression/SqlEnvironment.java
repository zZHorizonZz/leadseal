package dev.horizon.leadseal.expression;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The place that a {@link SqlExpression} writes itself into: the dialect of the SQL, and the qualifier of the columns of
 * each row that the expression can read, such as {@code NEW} for the new row in a trigger.
 *
 * <pre>{@code
 * SqlEnvironment check = SqlEnvironment.of(dialect);
 * SqlEnvironment trigger = SqlEnvironment.of(dialect).with(Row.CURRENT, "NEW").with(Row.NEW, "NEW").with(Row.OLD, "OLD");
 * }</pre>
 *
 * @param dialect the dialect
 * @param qualifiers the qualifier of each row that the expression can read, or an empty text for columns without a
 * qualifier
 */
public record SqlEnvironment(Dialect dialect, Map<Row, String> qualifiers) {

  public SqlEnvironment {
    Objects.requireNonNull(dialect, "dialect");
    qualifiers = Map.copyOf(qualifiers);
  }

  /**
   * Gives the environment of a check or a computed column, which reads the current row without a qualifier.
   *
   * @param dialect the dialect
   * @return the environment
   */
  public static SqlEnvironment of(Dialect dialect) {
    return new SqlEnvironment(dialect, Map.of(Row.CURRENT, ""));
  }

  /**
   * Gives this environment with one more row, or another qualifier of a row.
   *
   * @param row the row
   * @param qualifier the qualifier of its columns, or an empty text for none
   * @return the environment
   */
  public SqlEnvironment with(Row row, String qualifier) {
    Map<Row, String> all = new EnumMap<>(Row.class);
    all.putAll(qualifiers);
    all.put(row, Objects.requireNonNull(qualifier, "qualifier"));
    return new SqlEnvironment(dialect, all);
  }

  /**
   * Writes a column of a row, with the qualifier of the row.
   *
   * @param name the column name
   * @param row the row
   * @return the SQL, for example {@code NEW."total"}
   * @throws SchemaException if the expression cannot read the row here
   */
  public String column(String name, Row row) {
    String qualifier = qualifiers.get(row);
    if (qualifier == null) {
      throw new InvalidSchemaException("An expression reads the " + row.describe() + ", which it cannot read here.");
    }
    return (qualifier.isEmpty() ? "" : qualifier + ".") + dialect.quote(name);
  }
}
