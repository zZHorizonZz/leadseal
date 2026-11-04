package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;
import java.util.ArrayList;
import java.util.List;

record MariaDbTableDdl(Ddl ddl) implements TableDdl {

  @Override
  public List<Command> renameTable(TableTransition table, String name) {
    return List.of(new RenameTo(table.name(), table.name().withName(name)));
  }

  @Override
  public List<Command> moveTable(TableName table, Namespace.Name namespace) {
    List<Command> all = new ArrayList<>(namespace.map(this::createSchema, List::of));
    all.add(new RenameTable(table, table.inNamespace(namespace)));
    return all;
  }

  private record RenameTo(TableName origin, TableName target) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(origin).append(" RENAME TO ").table(target);
    }
  }

  private record RenameTable(TableName origin, TableName target) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("RENAME TABLE ").table(origin).append(" TO ").table(target);
    }
  }
}
