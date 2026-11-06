package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.Generated;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.CreateIndex;
import dev.horizon.leadseal.command.Command.CreateTable;
import java.util.List;
import java.util.Optional;

record SqliteRebuild(SqliteDdl ddl, Transition transition, String id) implements Command {

  @Override
  public void render(ScriptWriter script) {
    Table origin = transition.origin().tableById(id).orElseThrow();
    Table target = transition.target().tableById(id).orElseThrow();
    TableName source = transition.origin().nameOf(id).orElseThrow();
    TableName name = transition.target().nameOf(id).orElseThrow();
    TableName scratch = name.withName(script.internal("new_" + name.name()));
    script.render(new CreateTable(scratch, ddl.table().elements(target))).end();
    List<Column> copies = target.columns().stream().filter(column -> column.attribute(Generated.class).isEmpty())
        .filter(column -> origin.columnById(column.id()).isPresent() || column.attribute(Default.class).isEmpty())
        .toList();
    script.append("INSERT INTO ").table(scratch).append(" (").names(copies.stream().map(Column::name).toList())
        .append(") SELECT ");
    for (int i = 0; i < copies.size(); i++) {
      script.append((i == 0 ? "" : ", ") + copy(script.dialect(), origin, copies.get(i)));
    }
    script.append(" FROM ").table(source).end();
    script.append("DROP TABLE ").table(source).end();
    script.append("ALTER TABLE ").table(scratch).append(" RENAME TO ").name(name.name()).end();
    for (Index index : target.attributes(Index.class)) {
      script.render(new CreateIndex(name, index)).end();
    }
  }

  private String copy(Dialect dialect, Table origin, Column column) {
    return origin.columnById(column.id()).map(source -> !column.nullable() && source.nullable()
        ? "COALESCE(" + dialect.quote(source.name()) + ", " + dialect.fill(column) + ")"
        : dialect.quote(source.name()))
        .orElseGet(() -> column.nullable() ? "NULL" : dialect.zero(column.type()));
  }

  @Override
  public Optional<Object> mergeKey() {
    return Optional.of(List.of(SqliteRebuild.class, id));
  }
}
