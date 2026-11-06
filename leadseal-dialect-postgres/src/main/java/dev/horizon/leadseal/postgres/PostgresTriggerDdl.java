package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.TriggerDdl;
import dev.horizon.leadseal.expression.SqlEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

record PostgresTriggerDdl(PostgresDdl ddl) implements TriggerDdl {

  @Override
  public List<Command> createTriggers(TableTransition table, Triggers triggers) {
    List<Command> all = new ArrayList<>();
    if (!triggers.rewrites().isEmpty()) {
      SqlEnvironment environment = rowEnvironment(Mutation.UPDATE);
      StringBuilder body = new StringBuilder("BEGIN\n");
      for (Rewrite rewrite : triggers.ordered()) {
        body.append("  NEW.").append(ddl.dialect().quote(rewrite.column())).append(" := ")
            .append(rewrite.expression().value(environment)).append(";\n");
      }
      String name = triggers.rewriteName(table.name());
      all.add(new CreateFunction(table.name(), name, ddl.dialect().dollarQuote(body + "  RETURN NEW;\nEND;")));
      all.add(new CreateTrigger(table.name(), name, "BEFORE", Mutation.UPDATE));
    }
    for (Mutation mutation : triggers.mutations()) {
      SqlEnvironment environment = rowEnvironment(mutation);
      StringBuilder body = new StringBuilder("BEGIN\n");
      for (Trigger trigger : triggers.triggers(mutation)) {
        trigger.condition().ifPresentOrElse(when -> body.append("  IF ").append(when.condition(environment))
            .append(" THEN\n").append(actions(trigger, environment, "    ")).append("  END IF;\n"),
            () -> body.append(actions(trigger, environment, "  ")));
      }
      String name = triggers.name(table.name(), mutation);
      all.add(new CreateFunction(table.name(), name, ddl.dialect().dollarQuote(body + "  RETURN NULL;\nEND;")));
      all.add(new CreateTrigger(table.name(), name, "AFTER", mutation));
    }
    return all;
  }

  @Override
  public List<Command> dropTriggers(TableTransition table, Triggers triggers) {
    List<String> names = new ArrayList<>();
    if (!triggers.rewrites().isEmpty()) {
      names.add(triggers.rewriteName(table.name()));
    }
    triggers.mutations().forEach(mutation -> names.add(triggers.name(table.name(), mutation)));
    List<Command> all = new ArrayList<>();
    for (String name : names) {
      all.add(new DropTrigger(table.name(), name));
      all.add(new DropFunction(table.name(), name));
    }
    return all;
  }

  private String actions(Trigger trigger, SqlEnvironment environment, String indent) {
    return trigger.actions().stream().map(action -> action(action, environment, indent)).collect(Collectors.joining());
  }

  private String action(Trigger.Action action, SqlEnvironment environment, String indent) {
    return switch (action) {
      case Trigger.Insert insert -> indent + insert(insert, environment) + ";\n";
      case Trigger.Update update -> indent + update(update, environment) + ";\n";
      case Trigger.Delete delete -> indent + delete(delete, environment) + ";\n";
      case Trigger.Assert check -> indent + "IF " + check.condition().negation(environment) + " THEN\n" + indent
          + "  RAISE EXCEPTION USING MESSAGE = " + ddl.dialect().string(check.message()) + ", ERRCODE = 'P0001';\n"
          + indent + "END IF;\n";
    };
  }

  private record CreateFunction(TableName table, String name, String body) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE FUNCTION ").name(table.namespace(), name).append("() RETURNS trigger LANGUAGE plpgsql AS ")
          .append(body);
    }
  }

  private record CreateTrigger(TableName table, String name, String timing, Mutation mutation) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE TRIGGER ").name(name).append(" " + timing + " " + mutation.name() + " ON ").table(table)
          .append(" FOR EACH ROW EXECUTE FUNCTION ").name(table.namespace(), name).append("()");
    }
  }

  private record DropTrigger(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP TRIGGER ").name(name).append(" ON ").table(table);
    }
  }

  private record DropFunction(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP FUNCTION ").name(table.namespace(), name).append("()");
    }
  }
}
