package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

record SqlServerConstraintDdl(Ddl ddl) implements ConstraintDdl {

  @Override
  public List<Command> dropIndex(TableTransition table, Index index) {
    return List.of(new DropIndex(table.name(), index.name()));
  }

  @Override
  public Clause foreignKey(ForeignKey key) {
    return new ForeignKeyConstraint(key);
  }

  private record DropIndex(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP INDEX ").name(name).append(" ON ").table(table);
    }
  }

  private record ForeignKeyConstraint(ForeignKey key) implements Clause {

    @Override
    public void write(ScriptWriter script) {
      script.append("CONSTRAINT ").name(key.name()).append(" FOREIGN KEY (").names(key.columns())
          .append(") REFERENCES ").table(key.table()).append(" (").names(key.referencedColumns()).append(")")
          .append(key.onDelete().withoutRestrict().clause("ON DELETE"))
          .append(key.onUpdate().withoutRestrict().clause("ON UPDATE"));
    }
  }
}
