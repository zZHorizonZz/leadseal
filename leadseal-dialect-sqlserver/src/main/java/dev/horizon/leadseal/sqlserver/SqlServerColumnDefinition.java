package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.Generated;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.expression.SqlEnvironment;
import java.util.Optional;

record SqlServerColumnDefinition(Table table, Column column) implements Clause {

  @Override
  public void write(ScriptWriter script) {
    Optional<Generated> generation = column.attribute(Generated.class);
    if (generation.isPresent()) {
      script.name(column.name()).append(" AS CAST((" + generation.get().expression().value(SqlEnvironment.of(script
          .dialect())) + ") AS " + script.dialect().type(table, column) + ") PERSISTED");
      return;
    }
    script.clause(new SqlServerColumnType(table, column));
    if (column.attribute(Identity.class).isPresent()) {
      script.append(" IDENTITY(1, 1)");
    }
    script.append(column.nullable() ? " NULL" : " NOT NULL");
    column.attribute(Default.class).ifPresent(value -> script.append(" CONSTRAINT ")
        .name(new SqlServerDefaultName(table, column).value())
        .append(" DEFAULT " + script.dialect().defaultValue(column, value)));
  }
}
