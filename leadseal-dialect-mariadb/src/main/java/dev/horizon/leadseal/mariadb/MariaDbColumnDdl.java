package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.Generated;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.FillNulls;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.expression.SqlEnvironment;
import java.util.List;
import java.util.Optional;

record MariaDbColumnDdl(Ddl ddl) implements ColumnDdl {

  @Override
  public Clause definition(Table table, Column column) {
    return new ColumnDefinition(table, column);
  }

  @Override
  public List<Command> alterColumnType(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> setNotNull(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> dropNotNull(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> setDefault(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> dropDefault(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> addIdentity(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> dropIdentity(ColumnTransition column) {
    return modify(column);
  }

  @Override
  public List<Command> collate(ColumnTransition column, Collation collation) {
    return collation.appliesTo(ddl.dialect()) ? modify(column) : List.of();
  }

  List<Command> modify(ColumnTransition column) {
    Column target = column.definition();
    Command modify = new ModifyColumn(column.table().name(), target.name(),
        definition(column.table().definition(), target));
    boolean needsFill = !target.nullable() && column.origin().map(Column::nullable).orElse(false);
    return needsFill ? List.of(new FillNulls(column.table().name(), target), modify) : List.of(modify);
  }

  private record ColumnDefinition(Table table, Column column) implements Clause {

    @Override
    public void write(ScriptWriter script) {
      Dialect dialect = script.dialect();
      script.name(column.name()).append(" " + dialect.type(table, column));
      Optional<Generated> generation = column.attribute(Generated.class);
      if (generation.isPresent()) {
        script.append(" GENERATED ALWAYS AS (" + generation.get().expression().value(SqlEnvironment.of(dialect))
            + ") STORED");
        column.attribute(Comment.class)
            .ifPresent(comment -> script.append(" COMMENT " + dialect.string(comment.text())));
        return;
      }
      column.attributes(Collation.class).stream().filter(collation -> collation.appliesTo(dialect))
          .forEach(collation -> script.append(" COLLATE " + collation.bareName()));
      script.append(column.nullable() ? " NULL" : " NOT NULL");
      column.attribute(Default.class)
          .ifPresent(value -> script.append(" DEFAULT " + dialect.defaultValue(column, value)));
      if (column.attribute(Identity.class).isPresent()) {
        script.append(" AUTO_INCREMENT");
      }
      column.attribute(Comment.class)
          .ifPresent(comment -> script.append(" COMMENT " + dialect.string(comment.text())));
    }
  }

  private record ModifyColumn(TableName table, String column, Clause definition) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" MODIFY COLUMN ").clause(definition);
    }

    @Override
    public Optional<Object> mergeKey() {
      return Optional.of(List.of(ModifyColumn.class, table, column));
    }
  }
}
