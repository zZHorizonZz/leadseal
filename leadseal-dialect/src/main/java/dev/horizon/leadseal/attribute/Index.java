package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An index of a table.
 *
 * @param name the index name
 * @param columns the indexed columns, in order
 * @param unique {@code true} for an index that rejects two rows with the same values
 * @param descending the indexed columns that the index sorts in descending order
 * @param rows the rows that the index indexes: all, or the rows that meet a condition for a partial index
 */
public record Index(String name, List<String> columns, boolean unique, List<String> descending,
    @JsonProperty("where") Rows rows) implements TableAttribute {

  public Index {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("An index has no name.");
    }
    columns = List.copyOf(columns);
    descending = descending == null ? List.of() : List.copyOf(descending);
    rows = rows == null ? Rows.ALL : rows;
    if (columns.isEmpty()) {
      throw new InvalidSchemaException("The index '" + name + "' has no columns.");
    }
    for (String column : descending) {
      if (!columns.contains(column)) {
        throw new InvalidSchemaException(
            "The index '" + name + "' sorts the column '" + column + "' in descending order," + " but does not index it.");
      }
    }
  }

  /**
   * Returns an index with a generated name: {@code ix_} for an index or {@code ux_} for a unique index, followed by
   * the table name and the column names.
   *
   * @param table the table name
   * @param columns the indexed columns
   * @param unique {@code true} for a unique index
   * @return the index
   */
  public static Index of(String table, List<String> columns, boolean unique) {
    return new Index(new ConstraintName(unique ? "ux" : "ix", table, columns).value(), columns, unique,
        List.of(), Rows.ALL);
  }

  /**
   * Returns a copy of this index that sorts some columns in descending order.
   *
   * @param descending the columns to sort in descending order
   * @return the new index
   */
  public Index withDescending(List<String> descending) {
    return new Index(name, columns, unique, descending, rows);
  }

  /**
   * Returns a copy of this index that indexes only the rows that meet a condition.
   *
   * @param condition the SQL condition, or an empty text to index every row
   * @return the new index
   */
  public Index withWhere(String condition) {
    return new Index(name, columns, unique, descending, Rows.of(condition));
  }

  /**
   * Returns whether a column is in this index.
   *
   * @param column the column name
   * @return {@code true} if the index contains the column
   */
  public boolean contains(String column) {
    return columns.contains(column);
  }

  /** The rows that an index indexes: all rows, or the rows that meet a condition. */
  public sealed interface Rows {

    /** All rows. */
    Rows ALL = new All();

    /**
     * Gives the rows from their form in JSON.
     *
     * @param condition the SQL condition, or {@code null} or an empty text for all rows
     * @return the rows
     */
    @JsonCreator
    static Rows of(String condition) {
      return condition == null || condition.isBlank() ? ALL : new Where(condition);
    }

    /**
     * Gives a value for the condition of these rows or for all rows.
     *
     * @param where the value for the rows that meet a condition
     * @param all the value for all rows
     * @param <T> the type of the value
     * @return the value
     */
    <T> T map(Function<String, T> where, Supplier<T> all);

    /**
     * Tells if the index indexes only some rows.
     *
     * @return true for a partial index
     */
    default boolean partial() {
      return map(_ -> true, () -> false);
    }

    /**
     * Writes the end of {@code CREATE INDEX} for these rows.
     *
     * @return {@code " WHERE condition"}, or an empty text for all rows
     */
    default String clause() {
      return map(condition -> " WHERE " + condition, () -> "");
    }

    /**
     * Gives the form of these rows in JSON.
     *
     * @return the condition, or {@code null} for all rows, which JSON leaves out
     */
    @JsonValue
    default String json() {
      return map(condition -> condition, () -> null);
    }

    /** All rows. */
    record All() implements Rows {

      @Override
      public <T> T map(Function<String, T> where, Supplier<T> all) {
        return all.get();
      }
    }

    /**
     * The rows that meet a condition.
     *
     * @param condition the SQL condition
     */
    record Where(String condition) implements Rows {

      @Override
      public <T> T map(Function<String, T> where, Supplier<T> all) {
        return where.apply(condition);
      }
    }
  }

  @Override
  public Object key() {
    return List.of(Index.class, name);
  }

  @Override
  public String summary(String table) {
    return name;
  }

  @Override
  public void validate(Table table) {
    for (String column : columns) {
      if (table.column(column).isEmpty()) {
        throw new InvalidSchemaException("The index '" + name + "' refers to the column '" + column + "', which the table '"
            + table.name() + "' does not have.");
      }
    }
  }

  @Override
  public Optional<TableAttribute> rename(TableName table, Transition transition) {
    return transition.targetColumns(table, columns).flatMap(targetColumns -> transition.targetColumns(table, descending)
        .map(targetDescending -> new Index(name, targetColumns, unique, targetDescending, rows)));
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ADD_INDEXES;
  }

  @Override
  public Change.Phase dropPhase() {
    return Change.Phase.DROP_INDEXES;
  }

  @Override
  public List<Command> add(Ddl ddl, TableTransition table) {
    return ddl.constraint().addIndex(table, this);
  }

  @Override
  public List<Command> drop(Ddl ddl, TableTransition table) {
    return ddl.constraint().dropIndex(table, this);
  }

  @Override
  public boolean covers(String column) {
    return columns.contains(column);
  }
}
