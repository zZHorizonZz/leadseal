package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.FillNulls;
import java.util.ArrayList;
import java.util.List;

record SqlServerAlterColumn(SqlServerColumnDdl columns, ColumnTransition column) implements Command {

  @Override
  public void render(ScriptWriter script) {
    SqlServerDependents dependents = new SqlServerDependents(columns.ddl(), column);
    List<Command> all = new ArrayList<>(dependents.drops());
    all.addAll(alteration());
    all.addAll(dependents.primaryKey());
    column.definition().attribute(Default.class).ifPresent(_ -> all.addAll(columns.replaceDefault(column)));
    all.addAll(dependents.creates());
    all.forEach(command -> script.render(command).end());
  }

  private List<Command> alteration() {
    Column origin = column.origin().orElseThrow();
    Column target = column.definition();
    TableName table = column.table().name();
    List<Command> all = new ArrayList<>();
    if (origin.attribute(Default.class).isPresent()) {
      all.add(new SqlServerDropDefault(table, column.name()));
    }
    if (origin.nullable() && !target.nullable()) {
      all.add(new FillNulls(table, target));
    }
    all.add(new AlterType(table, new SqlServerColumnType(column.table().definition(), target), target.nullable()));
    return all;
  }

  private record AlterType(TableName table, Clause type, boolean nullable) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").clause(type)
          .append(nullable ? " NULL" : " NOT NULL");
    }
  }
}
