package dev.horizon.leadseal.mariadb;

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
import dev.horizon.leadseal.expression.SqlEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

record MariaDbTriggerDdl(Ddl ddl) implements TriggerDdl {

  @Override
  public List<Command> createTriggers(TableTransition table, Triggers triggers) {
    List<Command> all = new ArrayList<>();
    if (!triggers.rewrites().isEmpty()) {
      SqlEnvironment environment = rowEnvironment(Mutation.UPDATE);
      StringBuilder body = new StringBuilder("BEGIN\n");
      for (Rewrite rewrite : triggers.ordered()) {
        body.append("  SET NEW.").append(ddl.dialect().quote(rewrite.column())).append(" = ")
            .append(rewrite.expression().value(environment)).append(";\n");
      }
      all.add(new CreateTrigger(table.name(), triggers.rewriteName(table.name()), "BEFORE", Mutation.UPDATE,
          body + "END"));
    }
    for (Mutation mutation : triggers.mutations()) {
      SqlEnvironment environment = rowEnvironment(mutation);
      StringBuilder body = new StringBuilder("BEGIN\n");
      for (Trigger trigger : triggers.triggers(mutation)) {
        trigger.condition().ifPresentOrElse(when -> body.append("  IF ").append(when.condition(environment))
            .append(" THEN\n").append(actions(trigger, environment, "    ")).append("  END IF;\n"),
            () -> body.append(actions(trigger, environment, "  ")));
      }
      all.add(new CreateTrigger(table.name(), triggers.name(table.name(), mutation), "AFTER", mutation, body + "END"));
    }
    return all;
  }

  @Override
  public List<Command> dropTriggers(TableTransition table, Triggers triggers) {
    List<Command> all = new ArrayList<>();
    if (!triggers.rewrites().isEmpty()) {
      all.add(new DropTrigger(table.name(), triggers.rewriteName(table.name())));
    }
    triggers.mutations().forEach(mutation -> all.add(new DropTrigger(table.name(), triggers.name(table.name(),
        mutation))));
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
          + "  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = " + ddl.dialect().string(check.message()) + ";\n"
          + indent + "END IF;\n";
    };
  }

  private record CreateTrigger(TableName table, String name, String timing, Mutation mutation, String body)
      implements
        Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE TRIGGER ").name(table.namespace(), name).append(" " + timing + " " + mutation.name()
          + " ON ").table(table).append(" FOR EACH ROW " + body);
    }
  }

  private record DropTrigger(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP TRIGGER ").name(table.namespace(), name);
    }
  }
}
