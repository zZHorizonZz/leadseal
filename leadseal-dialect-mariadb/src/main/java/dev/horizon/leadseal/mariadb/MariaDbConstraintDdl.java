package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.AddConstraint;
import dev.horizon.leadseal.command.Command.DropConstraint;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

record MariaDbConstraintDdl(Ddl ddl) implements ConstraintDdl {

  @Override
  public List<Command> alterPrimaryKey(TableTransition table, PrimaryKey origin, PrimaryKey target) {
    return List.of(new ReplacePrimaryKey(table.name(), target));
  }

  @Override
  public List<Command> addIndex(TableTransition table, Index index) {
    return index.rows().map(condition -> {
      throw new UnsupportedChangeException("MariaDB has no partial indexes, so it cannot create the index '" + index.name()
          + "' with the condition " + condition + ".");
    }, () -> ConstraintDdl.super.addIndex(table, index));
  }

  @Override
  public List<Command> dropIndex(TableTransition table, Index index) {
    return List.of(new DropIndex(table.name(), index.name()));
  }

  @Override
  public List<Command> dropForeignKey(TableTransition table, ForeignKey key) {
    return List.of(new DropForeignKey(table.name(), key.name()));
  }

  private record ReplacePrimaryKey(TableName table, PrimaryKey key) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" DROP PRIMARY KEY, ADD PRIMARY KEY (").names(key.columns())
          .append(")");
    }
  }

  private record DropIndex(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP INDEX ").name(name).append(" ON ").table(table);
    }
  }

  private record DropForeignKey(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" DROP FOREIGN KEY ").name(name);
    }
  }

  @Override
  public List<Command> renameCheck(TableTransition table, Check origin, Check target) {
    return List.of(new DropConstraint(table.name(), origin.name()), new AddConstraint(table.name(),
        new Clause.CheckConstraint(target)));
  }
}
