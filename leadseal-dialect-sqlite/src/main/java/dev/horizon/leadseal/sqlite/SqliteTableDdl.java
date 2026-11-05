package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.CreateIndex;
import dev.horizon.leadseal.command.Command.CreateTable;
import dev.horizon.leadseal.command.Command.RenameTable;
import dev.horizon.leadseal.command.TableDdl;
import java.util.ArrayList;
import java.util.List;

record SqliteTableDdl(SqliteDdl ddl) implements TableDdl {

  @Override
  public List<Command> createSchema(String namespace) {
    throw new UnsupportedChangeException("SQLite has no namespaces, so it cannot create the namespace '" + namespace + "'.");
  }

  @Override
  public List<Command> createTable(TableTransition table, Table definition) {
    Table target = table.definition();
    List<Command> all = new ArrayList<>(table.name().namespace().map(this::createSchema, List::of));
    all.add(new CreateTable(table.name(), elements(target)));
    target.attributes(Index.class).forEach(index -> all.add(new CreateIndex(table.name(), index)));
    return all;
  }

  @Override
  public List<Command> renameTable(TableTransition table, String name) {
    return List.of(new RenameTable(table.name(), name));
  }

  @Override
  public List<Command> moveTable(TableName table, Namespace.Name namespace) {
    throw new UnsupportedChangeException("SQLite has no namespaces, so it cannot move the table '" + table.name() + "'.");
  }

  List<Clause> elements(Table table) {
    List<Clause> all = new ArrayList<>();
    table.columns().forEach(column -> all.add(ddl.column().definition(table, column)));
    all.add(new Clause.PrimaryKeyConstraint(table.primaryKey()));
    table.attributes(Check.class).forEach(check -> all.add(new Clause.CheckConstraint(check)));
    table.attributes(ForeignKey.class).forEach(key -> all.add(ddl.constraint().foreignKey(key)));
    return all;
  }
}
