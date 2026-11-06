package dev.horizon.leadseal.sqlite;

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
import java.util.stream.Collectors;

record SqliteTriggerDdl(Ddl ddl) implements TriggerDdl {

  @Override
  public List<Command> createTriggers(TableTransition table, Triggers triggers) {
    return triggers.runs().stream().<Command> map(mutation -> new CreateTrigger(table.name(), triggers.name(table
        .name(), mutation), mutation, body(table, triggers, mutation))).toList();
  }

  @Override
  public List<Command> dropTriggers(TableTransition table, Triggers triggers) {
    return triggers.runs().stream().<Command> map(mutation -> new DropTrigger(triggers.name(table.name(),
        mutation))).toList();
  }

  private String body(TableTransition table, Triggers triggers, Mutation mutation) {
    Dialect dialect = ddl.dialect();
    String own = dialect.quote(table.name());
    boolean present = mutation.rows().contains(Row.NEW);
    SqlEnvironment environment = SqlEnvironment.of(dialect).with(Row.CURRENT, present ? own : "OLD").with(Row.NEW, own)
        .with(Row.OLD, "OLD");
    String row = table.definition().primaryKey().columns().stream().map(column -> own + "." + dialect.quote(column)
        + " = NEW." + dialect.quote(column)).collect(Collectors.joining(" AND "));
    StringBuilder body = new StringBuilder();
    if (triggers.rewrites(mutation)) {
      for (List<Rewrite> level : triggers.levels()) {
        body.append("  UPDATE ").append(own).append(" SET ").append(level.stream().map(rewrite -> dialect.quote(rewrite
            .column()) + " = " + rewrite.expression().value(environment)).collect(Collectors.joining(", ")))
            .append(" WHERE ").append(row).append(";\n");
      }
    }
    for (Trigger trigger : triggers.triggers(mutation)) {
      List<String> filters = new ArrayList<>();
      if (present) {
        filters.add(row);
      }
      trigger.condition().ifPresent(when -> filters.add(when.condition(environment)));
      String source = present ? " FROM " + own : "";
      for (Trigger.Action action : trigger.actions()) {
        body.append(action(action, environment, source, filters));
      }
    }
    return body.toString();
  }

  private String action(Trigger.Action action, SqlEnvironment environment, String source, List<String> filters) {
    Dialect dialect = ddl.dialect();
    return switch (action) {
      case Trigger.Insert insert -> "  INSERT INTO " + dialect.quote(insert.table()) + " (" + insert.values().stream()
          .map(value -> dialect.quote(value.column())).collect(Collectors.joining(", ")) + ") SELECT "
          + insert
              .values().stream().map(value -> value.expression().value(environment)).collect(Collectors.joining(", "))
          + source + where(filters) + ";\n";
      case Trigger.Update update -> {
        SqlEnvironment target = environment.with(Row.CURRENT, dialect.quote(update.table()));
        List<String> all = new ArrayList<>(filters);
        all.add(update.where().condition(target));
        yield "  UPDATE " + dialect.quote(update.table()) + " SET " + update.values().stream().map(value -> dialect
            .quote(value.column()) + " = " + value.expression().value(target)).collect(Collectors.joining(", "))
            + source + where(all) + ";\n";
      }
      case Trigger.Delete delete -> {
        List<String> all = new ArrayList<>(filters);
        all.add(delete.where().condition(environment.with(Row.CURRENT, dialect.quote(delete.table()))));
        yield "  DELETE FROM " + dialect.quote(delete.table()) + (source.isEmpty() ? where(all)
            : " WHERE EXISTS (SELECT 1" + source + where(all) + ")") + ";\n";
      }
      case Trigger.Assert check -> {
        List<String> all = new ArrayList<>(filters);
        all.add(check.condition().negation(environment));
        yield "  SELECT RAISE(ABORT, " + dialect.string(check.message()) + ")" + source + where(all) + ";\n";
      }
    };
  }

  private String where(List<String> filters) {
    return filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
  }

  private record CreateTrigger(TableName table, String name, Mutation mutation, String body) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE TRIGGER ").name(name).append(" AFTER " + mutation.name() + " ON ").table(table)
          .append(" BEGIN\n" + body + "END");
    }
  }

  private record DropTrigger(String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP TRIGGER ").name(name);
    }
  }
}
