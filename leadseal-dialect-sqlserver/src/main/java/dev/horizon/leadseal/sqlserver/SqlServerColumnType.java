package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.command.Clause;

record SqlServerColumnType(Table table, Column column) implements Clause {

  @Override
  public void write(ScriptWriter script) {
    Dialect dialect = script.dialect();
    script.name(column.name()).append(" " + dialect.type(table, column));
    column.attributes(Collation.class).stream().filter(collation -> collation.appliesTo(dialect))
        .forEach(collation -> script.append(" COLLATE " + collation.bareName()));
  }
}
