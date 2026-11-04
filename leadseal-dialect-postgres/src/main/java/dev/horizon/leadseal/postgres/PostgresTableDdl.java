package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;
import java.util.ArrayList;
import java.util.List;

record PostgresTableDdl(Ddl ddl) implements TableDdl {

  @Override
  public List<Command> moveTable(TableName table, Namespace.Name namespace) {
    return namespace.map(text -> {
      List<Command> all = new ArrayList<>(createSchema(text));
      all.add(new SetSchema(table, text));
      return all;
    }, () -> List.of(new MoveToCurrentSchema(table)));
  }

  private record SetSchema(TableName table, String namespace) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" SET SCHEMA ").name(namespace);
    }
  }

  private record MoveToCurrentSchema(TableName table) implements Command {

    @Override
    public void render(ScriptWriter script) {
      String source = script.dialect().quote(table);
      script.append("DO $$ BEGIN EXECUTE format('ALTER TABLE " + source.replace("'", "''")
          + " SET SCHEMA %I', current_schema()); END $$");
    }
  }
}
