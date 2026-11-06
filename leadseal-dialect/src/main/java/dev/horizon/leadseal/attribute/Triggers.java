package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.expression.SqlExpression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The rewrites and the triggers of a table: what the database does on each mutation of its rows. The database runs
 * the rewrites of an update first, in the order of what they read, and then the triggers of the mutation, in their
 * order.
 *
 * <p>
 * A dialect writes them as the triggers of its database, at most one for each mutation and one for the rewrites, named
 * {@link #name}. A trigger reads its columns and tables by their names, so a migration that renames a column or a
 * table that they read, or that changes the primary key, drops the triggers of the table and creates them again, as it
 * does when the rewrites or the triggers change.
 *
 * @param rewrites the rewrites, at most one for each column
 * @param triggers the triggers, in the order they run, with distinct names
 */
public record Triggers(List<Rewrite> rewrites, List<Trigger> triggers) implements TableAttribute {

  public Triggers {
    rewrites = rewrites == null ? List.of() : List.copyOf(rewrites);
    triggers = triggers == null ? List.of() : List.copyOf(triggers);
    if (rewrites.isEmpty() && triggers.isEmpty()) {
      throw new InvalidSchemaException("A table has triggers without rewrites or triggers.");
    }
    Set<String> columns = new HashSet<>();
    for (Rewrite rewrite : rewrites) {
      if (!columns.add(rewrite.column())) {
        throw new InvalidSchemaException("The column " + rewrite.column() + " has two rewrites.");
      }
    }
    Set<String> names = new HashSet<>();
    for (Trigger trigger : triggers) {
      if (!names.add(trigger.name())) {
        throw new InvalidSchemaException("A table has two triggers named " + trigger.name() + ".");
      }
    }
  }

  /**
   * Gives the attribute of a table with rewrites and no triggers.
   *
   * @param rewrites the rewrites
   * @return the attribute
   */
  public static Triggers of(Rewrite... rewrites) {
    return new Triggers(List.of(rewrites), List.of());
  }

  /**
   * Gives the rewrites in levels. A level has the rewrites that read no rewritten column, or only the columns of the
   * levels before it, so a database that writes all the rewrites of a level in one statement reads the rewritten
   * values.
   *
   * @return the levels, in the order they run, each with its rewrites in the order of this attribute
   * @throws SchemaException if the rewrites read each other
   */
  public List<List<Rewrite>> levels() {
    Map<Rewrite, Integer> depths = new LinkedHashMap<>();
    rewrites.forEach(rewrite -> depth(rewrite, depths, new LinkedHashSet<>()));
    List<List<Rewrite>> levels = new ArrayList<>();
    for (int level = 0; depths.containsValue(level); level++) {
      int depth = level;
      levels.add(rewrites.stream().filter(rewrite -> depths.get(rewrite) == depth).toList());
    }
    return levels;
  }

  /**
   * Gives the rewrites in the order they run: a rewrite runs after the rewrites of the columns that it reads.
   *
   * @return the rewrites
   * @throws SchemaException if the rewrites read each other
   */
  public List<Rewrite> ordered() {
    return levels().stream().flatMap(List::stream).toList();
  }

  private int depth(Rewrite rewrite, Map<Rewrite, Integer> depths, Set<Rewrite> path) {
    Integer known = depths.get(rewrite);
    if (known != null) {
      return known;
    }
    if (!path.add(rewrite)) {
      throw new InvalidSchemaException("The rewrites read each other: " + path.stream().map(Rewrite::column)
          .collect(Collectors.joining(" -> ")) + " -> " + rewrite.column() + ".");
    }
    int depth = rewrites.stream().filter(rewrite::reads).mapToInt(dependency -> depth(dependency, depths, path) + 1)
        .max().orElse(0);
    path.remove(rewrite);
    depths.put(rewrite, depth);
    return depth;
  }

  /**
   * Gives the triggers that a mutation runs, in their order.
   *
   * @param mutation the mutation
   * @return the triggers
   */
  public List<Trigger> triggers(Mutation mutation) {
    return triggers.stream().filter(trigger -> trigger.on().contains(mutation)).toList();
  }

  /**
   * Tells if a mutation runs the rewrites.
   *
   * @param mutation the mutation
   * @return true if this attribute has rewrites and the mutation is an update
   */
  public boolean rewrites(Mutation mutation) {
    return !rewrites.isEmpty() && mutation == Mutation.UPDATE;
  }

  /**
   * Gives the mutations that run a rewrite or a trigger, for a dialect that writes the rewrites in the triggers of
   * their mutation.
   *
   * @return the mutations, in the order of {@link Mutation}
   */
  public List<Mutation> runs() {
    return Arrays.stream(Mutation.values()).filter(mutation -> rewrites(mutation) || !triggers(mutation).isEmpty())
        .toList();
  }

  /**
   * Gives the mutations that run a trigger.
   *
   * @return the mutations, in the order of {@link Mutation}
   */
  public List<Mutation> mutations() {
    return Arrays.stream(Mutation.values()).filter(mutation -> !triggers(mutation).isEmpty()).toList();
  }

  /**
   * Gives the tables whose rows the triggers write.
   *
   * @return the names of the tables
   */
  public Set<TableName> writes() {
    return triggers.stream().flatMap(trigger -> trigger.actions().stream()).flatMap(action -> action.writes().stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Gives the other tables that the rewrites and the triggers read or write.
   *
   * @return the names of the tables
   */
  public Set<TableName> tables() {
    Set<TableName> all = new LinkedHashSet<>(writes());
    expressions().flatMap(expression -> expression.foreignColumns().stream()).forEach(column -> all.add(column.table()));
    return all;
  }

  /**
   * Gives the name of the database trigger of a mutation.
   *
   * @param table the name of the table
   * @param mutation the mutation
   * @return the name, for example {@code tg_orders_update}
   */
  public String name(TableName table, Mutation mutation) {
    return new ConstraintName("tg", table.name(), List.of(mutation.text())).value();
  }

  /**
   * Gives the name of the database trigger of the rewrites, for a dialect that writes them apart from the triggers.
   *
   * @param table the name of the table
   * @return the name, for example {@code rw_orders}
   */
  public String rewriteName(TableName table) {
    return new ConstraintName("rw", table.name(), List.of()).value();
  }

  @Override
  public String summary(String table) {
    return new ConstraintName("tg", table, List.of()).value();
  }

  @Override
  public void validate(Table table) {
    rewrites.forEach(rewrite -> rewrite.validate(table));
    levels();
    triggers.forEach(trigger -> trigger.validate(table));
  }

  @Override
  public void validate(Table table, Schema schema) {
    TableName name = schema.nameOf(table.id()).orElseThrow();
    expressions().flatMap(expression -> expression.foreignColumns().stream()).forEach(column -> column.validate(schema));
    for (Trigger trigger : triggers) {
      try {
        trigger.actions().forEach(action -> action.validate(name, schema));
      } catch (SchemaException invalid) {
        throw new InvalidSchemaException("The trigger " + table.name() + "." + trigger.name() + " is not valid. "
            + invalid.getMessage(), invalid);
      }
    }
    Set<Node> visits = new HashSet<>();
    mutations().forEach(mutation -> new Node(name, mutation).visit(schema, new ArrayList<>(), new ArrayList<>(), visits));
  }

  private Stream<SqlExpression> expressions() {
    return Stream.concat(rewrites.stream().map(Rewrite::expression), triggers.stream()
        .flatMap(trigger -> trigger.expressions().stream()));
  }

  /**
   * A mutation of the rows of a table, which runs the triggers of the table on that mutation, and so the mutations of
   * the tables that their actions write.
   *
   * @param table the name of the table
   * @param mutation the mutation
   */
  private record Node(TableName table, Mutation mutation) {

    void visit(Schema schema, List<Node> stack, List<String> path, Set<Node> visits) {
      if (visits.contains(this)) {
        return;
      }
      int start = stack.indexOf(this);
      if (start >= 0) {
        throw new InvalidSchemaException("The triggers write in a loop: " + String.join(" -> ", path.subList(start,
            path.size())) + " -> " + table + ". A trigger that writes a row runs the triggers of that table, so the"
            + " loop would never end.");
      }
      stack.add(this);
      schema.table(table).flatMap(owner -> owner.attribute(Triggers.class)).ifPresent(attribute -> {
        for (Trigger trigger : attribute.triggers(mutation)) {
          for (Trigger.Action action : trigger.actions()) {
            action.writes().ifPresent(target -> action.mutation().ifPresent(write -> {
              path.add(table + "." + trigger.name());
              new Node(target, write).visit(schema, stack, path, visits);
              path.removeLast();
            }));
          }
        }
      });
      stack.removeLast();
      visits.add(this);
    }
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ADD_TRIGGERS;
  }

  @Override
  public Change.Phase dropPhase() {
    return Change.Phase.DROP_TRIGGERS;
  }

  @Override
  public Optional<TableAttribute> rename(TableName table, Transition transition) {
    Table origin = transition.origin().table(table).orElseThrow();
    Set<String> reads = new LinkedHashSet<>(origin.primaryKey().columns());
    rewrites.forEach(rewrite -> reads.addAll(rewrite.reads()));
    triggers.forEach(trigger -> reads.addAll(trigger.reads()));
    Map<TableName, Set<String>> tables = new LinkedHashMap<>();
    expressions().flatMap(expression -> expression.foreignColumns().stream()).forEach(column -> tables.computeIfAbsent(
        column.table(), _ -> new LinkedHashSet<>()).addAll(List.of(column.reference(), column.column())));
    triggers.stream().flatMap(trigger -> trigger.actions().stream()).forEach(action -> action.writes().ifPresent(
        target -> tables.computeIfAbsent(target, _ -> new LinkedHashSet<>()).addAll(action.columns())));
    tables.computeIfAbsent(table, _ -> new LinkedHashSet<>()).addAll(reads);
    boolean same = transition.targetName(table).flatMap(name -> transition.target().table(name))
        .filter(target -> key(target).equals(key(origin)) && target.columns().equals(origin.columns())).isPresent()
        && tables.entrySet().stream().allMatch(entry -> same(transition, entry.getKey(), entry.getValue()));
    return same ? Optional.of(this) : Optional.empty();
  }

  private List<Object> key(Table table) {
    return table.primaryKey().columns().stream().<Object> map(column -> List.of(column, table.column(column)
        .orElseThrow().type())).toList();
  }

  private boolean same(Transition transition, TableName table, Set<String> columns) {
    return transition.targetName(table).filter(table::equals).isPresent() && columns.stream()
        .allMatch(column -> transition.targetColumn(table, column).filter(column::equals).isPresent());
  }

  @Override
  public List<Command> add(Ddl ddl, TableTransition table) {
    return ddl.trigger().createTriggers(table, this);
  }

  @Override
  public List<Command> drop(Ddl ddl, TableTransition table) {
    return ddl.trigger().dropTriggers(table, this);
  }
}
