package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.command.Clause;

record SqlServerColumnDefinition(Table table, Column column) implements Clause {

  @Override
  public void write(ScriptWriter script) {
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
