package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.AddColumn;
import java.util.List;

record SqliteColumnDdl(SqliteDdl ddl) implements ColumnDdl {

  @Override
  public Clause definition(Table table, Column column) {
    return new ColumnDefinition(table, column);
  }

  @Override
  public List<Command> addColumn(ColumnTransition column) {
    Column definition = column.definition();
    if (definition.attribute(Default.class).filter(value -> !value.constant()).isPresent()) {
      return rebuild(column);
    }
    Column filled = definition.needsFill() ? definition.with(new LiteralDefault(definition.type().zero()))
        : definition;
    return List.of(new AddColumn(column.table().name(), definition(column.table().definition(), filled)));
  }

  @Override
  public List<Command> dropColumn(ColumnTransition column) {
    Table origin = column.table().origin().orElseThrow();
    boolean keyed = origin.primaryKey().columns().contains(column.name()) || origin.attributes(ForeignKey.class)
        .stream().anyMatch(key -> key.columns().contains(column.name()));
    return keyed ? rebuild(column) : ColumnDdl.super.dropColumn(column);
  }

  @Override
  public List<Command> alterColumnType(ColumnTransition column) {
    return rebuild(column);
  }

  @Override
  public List<Command> setNotNull(ColumnTransition column) {
    return rebuild(column);
  }

  @Override
  public List<Command> dropNotNull(ColumnTransition column) {
    return rebuild(column);
  }

  @Override
  public List<Command> setDefault(ColumnTransition column) {
    return rebuild(column);
  }

  @Override
  public List<Command> dropDefault(ColumnTransition column) {
    return rebuild(column);
  }

  @Override
  public List<Command> addIdentity(ColumnTransition column) {
    return List.of();
  }

  @Override
  public List<Command> dropIdentity(ColumnTransition column) {
    return List.of();
  }

  @Override
  public List<Command> collate(ColumnTransition column, Collation collation) {
    return collation.appliesTo(ddl.dialect()) ? rebuild(column) : List.of();
  }

  private List<Command> rebuild(ColumnTransition column) {
    return List.of(new SqliteRebuild(ddl, column.table().transition(), column.table().id()));
  }

  private record ColumnDefinition(Table table, Column column) implements Clause {

    @Override
    public void write(ScriptWriter script) {
      Dialect dialect = script.dialect();
      script.name(column.name()).append(" " + dialect.type(table, column));
      column.attributes(Collation.class).stream().filter(collation -> collation.appliesTo(dialect))
          .forEach(collation -> script.append(" COLLATE " + collation.bareName()));
      if (!column.nullable()) {
        script.append(" NOT NULL");
      }
      column.attribute(Default.class)
          .ifPresent(value -> script.append(" DEFAULT " + dialect.defaultValue(column, value)));
    }
  }
}
