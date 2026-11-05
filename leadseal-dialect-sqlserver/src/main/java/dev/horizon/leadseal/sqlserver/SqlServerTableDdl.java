package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;
import java.util.ArrayList;
import java.util.List;

record SqlServerTableDdl(Ddl ddl) implements TableDdl {

  @Override
  public List<Command> createSchema(String namespace) {
    return List.of(new CreateSchema(namespace));
  }

  @Override
  public List<Command> renameTable(TableTransition table, String name) {
    Dialect dialect = ddl.dialect();
    String origin = table.origin().orElseThrow().primaryKey().name();
    String target = table.definition().primaryKey().name();
    List<Command> all = new ArrayList<>(List.of(new SqlServerRename(dialect.quote(table.name()), name, "")));
    if (!origin.equals(target)) {
      all.add(new SqlServerRename(dialect.quote(table.name().withName(origin)), target, ", N'OBJECT'"));
    }
    return all;
  }

  @Override
  public List<Command> moveTable(TableName table, Namespace.Name namespace) {
    return namespace.map(text -> {
      List<Command> all = new ArrayList<>(createSchema(text));
      all.add(new Transfer(table, text));
      return all;
    }, () -> List.of(new MoveToDefaultSchema(table)));
  }

  private record CreateSchema(String namespace) implements Command {

    @Override
    public void render(ScriptWriter script) {
      Dialect dialect = script.dialect();
      script.append("IF SCHEMA_ID(" + dialect.string(namespace) + ") IS NULL EXEC("
          + dialect.string("CREATE SCHEMA " + dialect.quote(namespace)) + ")");
    }
  }

  private record Transfer(TableName table, String namespace) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER SCHEMA ").name(namespace).append(" TRANSFER ").table(table);
    }
  }

  private record MoveToDefaultSchema(TableName table) implements Command {

    @Override
    public void render(ScriptWriter script) {
      String move = SqlServerVariable.of(script, "move").name();
      script.append("DECLARE " + move + " nvarchar(max) = N'ALTER SCHEMA ' + QUOTENAME(SCHEMA_NAME()) + N' TRANSFER ' + "
          + script.dialect().string(script.dialect().quote(table)) + " EXEC(" + move + ")");
    }
  }
}
