package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A trigger of a table: actions that the database runs after each insert, update or delete of a row, for the rows
 * that meet a condition. An action writes rows of another table, or refuses the mutation. A trigger cannot change the
 * rows of its own table: a {@link Rewrite} changes the values of the row.
 *
 * <p>
 * The expressions of a trigger read the row after the mutation, with its rewritten values, and on an update or a
 * delete the row before it. The actions run in their order, for each row.
 *
 * @param name the name, unique in its table
 * @param on the mutations that run the trigger, at least one
 * @param when the condition that a row must meet to run the actions, or {@link #ALWAYS}
 * @param actions the actions, at least one, in the order they run
 */
public record Trigger(String name, List<Mutation> on, SqlExpression when, List<Action> actions) {

  public static final SqlExpression ALWAYS = new Literal(new BooleanType(), "true");

  public Trigger {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("A trigger has no name.");
    }
    on = List.copyOf(on);
    if (on.isEmpty()) {
      throw new InvalidSchemaException("The trigger " + name + " runs on no mutation.");
    }
    if (new HashSet<>(on).size() != on.size()) {
      throw new InvalidSchemaException("The trigger " + name + " runs twice on one mutation.");
    }
    Objects.requireNonNull(when, "when");
    actions = List.copyOf(actions);
    if (actions.isEmpty()) {
      throw new InvalidSchemaException("The trigger " + name + " has no action.");
    }
  }

  /**
   * Gives the rows that the expressions of this trigger read: the rows that each of its mutations has.
   *
   * @return the rows
   */
  public Set<Row> rows() {
    Set<Row> all = EnumSet.allOf(Row.class);
    on.forEach(mutation -> all.retainAll(mutation.rows()));
    return all;
  }

  /**
   * Gives the condition of this trigger.
   *
   * @return the condition, or empty for a trigger that runs for every row
   */
  public Optional<SqlExpression> condition() {
    return when.equals(ALWAYS) ? Optional.empty() : Optional.of(when);
  }

  /**
   * Gives the expressions of this trigger: its condition and the expressions of its actions.
   *
   * @return the expressions
   */
  public List<SqlExpression> expressions() {
    return Stream.concat(Stream.of(when), actions.stream().flatMap(action -> action.expressions().stream())).toList();
  }

  /**
   * Gives the columns of its own table that this trigger reads.
   *
   * @return the names of the columns
   */
  public Set<String> reads() {
    Set<String> all = new LinkedHashSet<>();
    when.columns().forEach(reference -> all.add(reference.name()));
    actions.forEach(action -> all.addAll(action.reads()));
    return all;
  }

  void validate(Table table) {
    try {
      when.validate(table, rows());
      actions.forEach(action -> action.validate(table, rows()));
    } catch (SchemaException invalid) {
      throw new InvalidSchemaException("The trigger " + table.name() + "." + name + " is not valid. " + invalid
          .getMessage(), invalid);
    }
  }

  /** An action of a trigger. */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = Insert.class, name = "insert"),
      @JsonSubTypes.Type(value = Update.class, name = "update"),
      @JsonSubTypes.Type(value = Delete.class, name = "delete"),
      @JsonSubTypes.Type(value = Assert.class, name = "assert")
  })
  public sealed interface Action permits Insert, Update, Delete, Assert {

    /**
     * Gives the expressions of this action.
     *
     * @return the expressions
     */
    List<SqlExpression> expressions();

    /**
     * Gives the table whose rows this action writes.
     *
     * @return the table, or empty for an action that writes no row
     */
    Optional<TableName> writes();

    /**
     * Gives the mutation that this action runs on the rows of the table of {@link #writes()}, which runs the triggers
     * of that table on the mutation.
     *
     * @return the mutation, or empty for an action that writes no row
     */
    Optional<Mutation> mutation();

    /**
     * Gives the columns of the table of {@link #writes()} that this action writes or reads.
     *
     * @return the names of the columns, or none for an action that writes no row
     */
    Set<String> columns();

    /**
     * Gives the columns of the table of the trigger that this action reads.
     *
     * @return the names of the columns
     */
    Set<String> reads();

    /**
     * Checks that this action fits the table of its trigger.
     *
     * @param table the table of the trigger
     * @param rows the rows that the trigger reads
     * @throws SchemaException if the action reads a column or a row that it cannot read
     */
    void validate(Table table, Set<Row> rows);

    /**
     * Checks that this action fits the other tables of the schema.
     *
     * @param owner the name of the table of the trigger
     * @param schema the schema
     * @throws SchemaException if the action writes into a table or a column that it cannot write into
     */
    default void validate(TableName owner, Schema schema) {
    }
  }

  /**
   * Writes a row into another table. Its expressions read the row of the trigger.
   *
   * @param table the other table
   * @param values the value of each column that the row has, by column name
   */
  public record Insert(TableName table, List<Assignment> values) implements Action {

    public Insert {
      Objects.requireNonNull(table, "table");
      values = List.copyOf(values);
      if (values.isEmpty()) {
        throw new InvalidSchemaException("An insert into " + table + " writes no column.");
      }
    }

    @Override
    public List<SqlExpression> expressions() {
      return values.stream().map(Assignment::expression).toList();
    }

    @Override
    public Optional<TableName> writes() {
      return Optional.of(table);
    }

    @Override
    public Optional<Mutation> mutation() {
      return Optional.of(Mutation.INSERT);
    }

    @Override
    public Set<String> columns() {
      return values.stream().map(Assignment::column).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<String> reads() {
      return expressions().stream().flatMap(expression -> expression.columns().stream()).map(ColumnReference::name)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void validate(Table owner, Set<Row> rows) {
      expressions().forEach(expression -> expression.validate(owner, rows));
    }

    @Override
    public void validate(TableName owner, Schema schema) {
      Table target = new Target(table).table(owner, schema);
      Set<String> columns = new HashSet<>();
      for (Assignment value : values) {
        if (target.column(value.column()).isEmpty()) {
          throw new InvalidSchemaException("A trigger of the table " + owner + " writes the column " + table + "."
              + value.column() + ", which the table does not have.");
        }
        new Target(table).refuseGenerated(owner, target.column(value.column()).orElseThrow());
        if (!columns.add(value.column())) {
          throw new InvalidSchemaException("A trigger of the table " + owner + " writes the column " + table + "."
              + value.column() + " twice.");
        }
      }
      for (Column column : target.columns()) {
        if (!columns.contains(column.name()) && !column.nullable() && column.attribute(Default.class).isEmpty()
            && column.attribute(Identity.class).isEmpty()) {
          throw new InvalidSchemaException("A trigger of the table " + owner + " writes a row into " + table + " without"
              + " the column " + column.name() + ", which is NOT NULL and has no default. Give it a value.");
        }
      }
    }
  }

  /**
   * Changes the rows of another table that meet a condition. Its expressions read the columns of the other table by
   * their names, as {@link Row#CURRENT}, and the row of the trigger as {@link Row#NEW} and {@link Row#OLD}.
   *
   * @param table the other table
   * @param where the condition that a row of the other table must meet to change
   * @param values the new value of each column that changes, by column name
   */
  public record Update(TableName table, SqlExpression where, List<Assignment> values) implements Action {

    public Update {
      Objects.requireNonNull(table, "table");
      Objects.requireNonNull(where, "where");
      values = List.copyOf(values);
      if (values.isEmpty()) {
        throw new InvalidSchemaException("An update of " + table + " changes no column.");
      }
    }

    @Override
    public List<SqlExpression> expressions() {
      return Stream.concat(Stream.of(where), values.stream().map(Assignment::expression)).toList();
    }

    @Override
    public Optional<TableName> writes() {
      return Optional.of(table);
    }

    @Override
    public Optional<Mutation> mutation() {
      return Optional.of(Mutation.UPDATE);
    }

    @Override
    public Set<String> columns() {
      Set<String> all = values.stream().map(Assignment::column).collect(Collectors.toCollection(LinkedHashSet::new));
      all.addAll(new Target(table).reads(expressions()));
      return all;
    }

    @Override
    public Set<String> reads() {
      return new Target(table).trigger(expressions());
    }

    @Override
    public void validate(Table owner, Set<Row> rows) {
      new Target(table).validate(owner, rows, expressions());
    }

    @Override
    public void validate(TableName owner, Schema schema) {
      Target target = new Target(table);
      Table destination = target.table(owner, schema);
      target.validate(destination, expressions());
      Set<String> columns = new HashSet<>();
      for (Assignment value : values) {
        if (destination.column(value.column()).isEmpty() || !columns.add(value.column())) {
          throw new InvalidSchemaException("A trigger of the table " + owner + " changes the column " + table + "."
              + value.column() + ", which the table does not have or which it changes twice.");
        }
        target.refuseGenerated(owner, destination.column(value.column()).orElseThrow());
      }
    }
  }

  /**
   * Deletes the rows of another table that meet a condition. The condition reads the columns of the other table by
   * their names, as {@link Row#CURRENT}, and the row of the trigger as {@link Row#NEW} and {@link Row#OLD}.
   *
   * @param table the other table
   * @param where the condition that a row of the other table must meet to be deleted
   */
  public record Delete(TableName table, SqlExpression where) implements Action {

    public Delete {
      Objects.requireNonNull(table, "table");
      Objects.requireNonNull(where, "where");
    }

    @Override
    public List<SqlExpression> expressions() {
      return List.of(where);
    }

    @Override
    public Optional<TableName> writes() {
      return Optional.of(table);
    }

    @Override
    public Optional<Mutation> mutation() {
      return Optional.of(Mutation.DELETE);
    }

    @Override
    public Set<String> columns() {
      return new Target(table).reads(expressions());
    }

    @Override
    public Set<String> reads() {
      return new Target(table).trigger(expressions());
    }

    @Override
    public void validate(Table owner, Set<Row> rows) {
      new Target(table).validate(owner, rows, expressions());
    }

    @Override
    public void validate(TableName owner, Schema schema) {
      Target target = new Target(table);
      target.validate(target.table(owner, schema), expressions());
    }
  }

  /**
   * Refuses the mutation when a condition does not hold, with a message. A condition that is {@code NULL} holds, as
   * it does in a check.
   *
   * @param condition the condition that every row must meet
   * @param message the message of the error
   */
  public record Assert(SqlExpression condition, String message) implements Action {

    public Assert {
      Objects.requireNonNull(condition, "condition");
      if (message == null || message.isBlank()) {
        throw new InvalidSchemaException("An assert has no message.");
      }
    }

    @Override
    public List<SqlExpression> expressions() {
      return List.of(condition);
    }

    @Override
    public Optional<TableName> writes() {
      return Optional.empty();
    }

    @Override
    public Optional<Mutation> mutation() {
      return Optional.empty();
    }

    @Override
    public Set<String> columns() {
      return Set.of();
    }

    @Override
    public Set<String> reads() {
      return condition.columns().stream().map(ColumnReference::name).collect(Collectors.toCollection(
          LinkedHashSet::new));
    }

    @Override
    public void validate(Table owner, Set<Row> rows) {
      condition.validate(owner, rows);
    }
  }

  /**
   * The value of a column of a row that a trigger writes.
   *
   * @param column the name of the column
   * @param expression the value
   */
  public record Assignment(String column, SqlExpression expression) {

    public Assignment {
      Objects.requireNonNull(expression, "expression");
      if (column == null || column.isBlank()) {
        throw new InvalidSchemaException("A value of a trigger has no column.");
      }
    }
  }

  /**
   * The other table of an update or a delete, whose expressions read its columns as {@link Row#CURRENT} and the row of
   * the trigger as {@link Row#NEW} and {@link Row#OLD}.
   *
   * @param name the name of the other table
   */
  private record Target(TableName name) {

    void refuseGenerated(TableName owner, Column column) {
      if (column.attribute(Identity.class).isPresent() || column.attribute(Generated.class).isPresent()) {
        throw new InvalidSchemaException("A trigger of the table " + owner + " writes the column " + name + "."
            + column.name() + ", which is " + (column.attribute(Identity.class).isPresent() ? "an identity" : "a generated")
            + " column. The database writes its values, so leave it out.");
      }
    }

    Table table(TableName owner, Schema schema) {
      if (name.equals(owner)) {
        throw new InvalidSchemaException("A trigger of the table " + owner + " writes into its own table. A trigger"
            + " cannot change the rows of its table: use a rewrite for the values of the row.");
      }
      return schema.table(name).orElseThrow(() -> new InvalidSchemaException("A trigger of the table " + owner
          + " writes into the table " + name + ", which the schema does not have."));
    }

    Set<String> reads(List<SqlExpression> expressions) {
      return references(expressions).filter(reference -> reference.row() == Row.CURRENT).map(ColumnReference::name)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    Set<String> trigger(List<SqlExpression> expressions) {
      return references(expressions).filter(reference -> reference.row() != Row.CURRENT).map(ColumnReference::name)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    void validate(Table owner, Set<Row> rows, List<SqlExpression> expressions) {
      references(expressions).filter(reference -> reference.row() != Row.CURRENT)
          .forEach(reference -> reference.validate(owner, rows));
    }

    void validate(Table target, List<SqlExpression> expressions) {
      references(expressions).filter(reference -> reference.row() == Row.CURRENT)
          .forEach(reference -> reference.validate(target, Set.of(Row.CURRENT)));
    }

    private Stream<ColumnReference> references(List<SqlExpression> expressions) {
      return expressions.stream().flatMap(expression -> expression.columns().stream());
    }
  }
}
