package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TriggerDdl;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

record SqlServerTriggerDdl(Ddl ddl) implements TriggerDdl {

  @Override
  public List<Command> createTriggers(TableTransition table, Triggers triggers) {
    return triggers.runs().stream().<Command> map(mutation -> new CreateTrigger(table.name(), triggers.name(table
        .name(), mutation), mutation, script -> body(script, table, triggers, mutation))).toList();
  }

  @Override
  public List<Command> dropTriggers(TableTransition table, Triggers triggers) {
    return triggers.runs().stream().<Command> map(mutation -> new DropTrigger(table.name(), triggers.name(table.name(),
        mutation))).toList();
  }

  private String body(ScriptWriter script, TableTransition table, Triggers triggers, Mutation mutation) {
    Dialect dialect = ddl.dialect();
    String target = dialect.quote("t");
    String inserts = dialect.quote("i");
    String old = dialect.quote("d");
    boolean present = mutation.rows().contains(Row.NEW);
    SqlEnvironment environment = SqlEnvironment.of(dialect).with(Row.CURRENT, present ? target : old)
        .with(Row.NEW, target).with(Row.OLD, old);
    List<String> key = table.definition().primaryKey().columns();
    String source = present ? dialect.quote(table.name()) + " AS " + target + " JOIN inserted AS " + inserts + " ON "
        + join(key, target, inserts) + (mutation.rows().contains(Row.OLD) ? " LEFT JOIN deleted AS " + old + " ON "
            + join(key, target, old) : "")
        : "deleted AS " + old;
    StringBuilder body = new StringBuilder("BEGIN\n  SET NOCOUNT ON;\n  IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;\n");
    if (triggers.rewrites(mutation)) {
      for (List<Rewrite> level : triggers.levels()) {
        body.append("  UPDATE ").append(target).append(" SET ").append(level.stream().map(rewrite -> dialect.quote(
            rewrite.column()) + " = " + rewrite.expression().value(environment)).collect(Collectors.joining(", ")))
            .append(" FROM ").append(source).append(";\n");
      }
    }
    Loop loop = new Loop(dialect, script.internal("row"), present ? target : old, key);
    if (triggers.triggers(mutation).stream().flatMap(trigger -> trigger.actions().stream()).anyMatch(loop::needs)) {
      body.append(loop.declare(key.stream().map(column -> dialect.quote(column) + " " + dialect.type(table
          .definition(), table.definition().column(column).orElseThrow())).collect(Collectors.joining(", ")),
          present ? "inserted" : "deleted"));
    }
    for (Trigger trigger : triggers.triggers(mutation)) {
      List<String> filters = new ArrayList<>(trigger.condition().map(when -> when.condition(environment)).stream()
          .toList());
      for (Trigger.Action action : trigger.actions()) {
        body.append(action(action, environment, source, filters, loop));
      }
    }
    return body.append("END").toString();
  }

  private String action(Trigger.Action action, SqlEnvironment environment, String source, List<String> filters,
      Loop loop) {
    Dialect dialect = ddl.dialect();
    String alias = dialect.quote("x");
    return switch (action) {
      case Trigger.Insert insert -> "  INSERT INTO " + dialect.quote(insert.table()) + " (" + insert.values().stream()
          .map(value -> dialect.quote(value.column())).collect(Collectors.joining(", ")) + ") SELECT "
          + insert
              .values().stream().map(value -> value.expression().value(environment)).collect(Collectors.joining(", "))
          + " FROM " + source + where(filters) + ";\n";
      case Trigger.Update update -> {
        SqlEnvironment row = environment.with(Row.CURRENT, alias);
        List<String> all = new ArrayList<>(filters);
        all.add(update.where().condition(row));
        yield loop.each("UPDATE " + alias + " SET " + update.values().stream().map(value -> dialect.quote(value
            .column()) + " = " + value.expression().value(row)).collect(Collectors.joining(", ")) + " FROM " + dialect
                .quote(update.table())
            + " AS " + alias + ", " + source, all);
      }
      case Trigger.Delete delete -> {
        List<String> all = new ArrayList<>(filters);
        all.add(delete.where().condition(environment.with(Row.CURRENT, alias)));
        yield loop.each("DELETE " + alias + " FROM " + dialect.quote(delete.table()) + " AS " + alias + ", " + source,
            all);
      }
      case Trigger.Assert check -> {
        List<String> all = new ArrayList<>(filters);
        all.add(check.condition().negation(environment));
        yield "  IF EXISTS (SELECT 1 FROM " + source + where(all) + ") THROW 50000, " + dialect.string(check.message())
            + ", 1;\n";
      }
    };
  }

  private String where(List<String> filters) {
    return filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
  }

  private String join(List<String> key, String left, String right) {
    Dialect dialect = ddl.dialect();
    return key.stream().map(column -> left + "." + dialect.quote(column) + " = " + right + "." + dialect.quote(column))
        .collect(Collectors.joining(" AND "));
  }

  private record Loop(Dialect dialect, String column, String row, List<String> key) {

    boolean needs(Trigger.Action action) {
      return switch (action) {
        case Trigger.Update _,Trigger.Delete _ -> true;
        case Trigger.Insert _,Trigger.Assert _ -> false;
      };
    }

    String declare(String columns, String source) {
      String names = key.stream().map(dialect::quote).collect(Collectors.joining(", "));
      return "  DECLARE @rows TABLE (" + dialect.quote(column) + " INT IDENTITY(1, 1) PRIMARY KEY, " + columns + ");\n"
          + "  INSERT INTO @rows (" + names + ") SELECT " + names + " FROM " + source + ";\n"
          + "  DECLARE @row INT;\n  DECLARE @count INT = (SELECT COUNT(*) FROM @rows);\n";
    }

    String each(String statement, List<String> filters) {
      String number = dialect.quote("n");
      List<String> all = new ArrayList<>(filters);
      all.addFirst(number + "." + dialect.quote(column) + " = @row");
      return "  SET @row = 1;\n  WHILE @row <= @count\n  BEGIN\n    " + statement + " JOIN @rows AS "
          + number + " ON " + key.stream().map(name -> number + "." + dialect.quote(name) + " = " + row + "." + dialect
              .quote(name)).collect(Collectors.joining(" AND "))
          + " WHERE " + String.join(" AND ", all) + ";\n"
          + "    SET @row = @row + 1;\n  END;\n";
    }
  }

  private record CreateTrigger(TableName table, String name, Mutation mutation, Function<ScriptWriter, String> body)
      implements
        Command {

    @Override
    public void render(ScriptWriter script) {
      script.isolate().append("CREATE TRIGGER ").name(table.namespace(), name).append(" ON ").table(table)
          .append(" AFTER " + mutation.name() + " AS\n" + body.apply(script));
    }
  }

  private record DropTrigger(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP TRIGGER ").name(table.namespace(), name);
    }
  }
}
