package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.AddConstraint;
import dev.horizon.leadseal.command.Command.DropColumn;
import dev.horizon.leadseal.command.Command.DropConstraint;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

record SqlServerColumnDdl(Ddl ddl) implements ColumnDdl {

  @Override
  public Clause definition(Table table, Column column) {
    return new SqlServerColumnDefinition(table, column);
  }

  @Override
  public List<Command> renameColumn(ColumnTransition column, String origin) {
    Dialect dialect = ddl.dialect();
    String path = dialect.quote(column.table().name()) + "." + dialect.quote(origin);
    TableName table = column.table().name();
    Type type = column.origin().orElseThrow().type();
    Optional<Check> check = dialect.typeCheck(table.name(), origin, type);
    List<Command> all = new ArrayList<>();
    // SQL Server cannot rename a column that a check reads. The check comes back under its old name, and the rename of
    // the change renames it after.
    check.ifPresent(originCheck -> all.add(new DropConstraint(table, originCheck.name())));
    all.add(new SqlServerRename(path, column.name(), ", N'COLUMN'"));
    check.ifPresent(originCheck -> all.add(new AddConstraint(table, new Clause.CheckConstraint(new Check(
        originCheck.name(), dialect.typeCheck(table.name(), column.name(), type).orElseThrow().expression())))));
    return all;
  }

  @Override
  public List<Command> addColumn(ColumnTransition column) {
    TableName name = column.table().name();
    Table table = column.table().definition();
    Column definition = column.definition();
    List<Command> all = new ArrayList<>();
    if (definition.needsFill()) {
      all.add(new AddColumn(name, definition(table, definition.with(new LiteralDefault(definition.type().zero())))));
      all.add(new DropConstraint(name, new SqlServerDefaultName(table, definition).value()));
    } else {
      all.add(new AddColumn(name, definition(table, definition)));
    }
    if (definition.attribute(Comment.class).isPresent()) {
      all.addAll(ddl.comment().commentColumn(column));
    }
    return all;
  }

  @Override
  public List<Command> dropColumn(ColumnTransition column) {
    List<Command> all = new ArrayList<>();
    if (column.origin().flatMap(origin -> origin.attribute(Default.class)).isPresent()) {
      all.add(new SqlServerDropDefault(column.table().name(), column.name()));
    }
    all.add(new DropColumn(column.table().name(), column.name()));
    return all;
  }

  @Override
  public List<Command> alterColumnType(ColumnTransition column) {
    return List.of(new SqlServerAlterColumn(this, column));
  }

  @Override
  public List<Command> setNotNull(ColumnTransition column) {
    return List.of(new SqlServerAlterColumn(this, column));
  }

  @Override
  public List<Command> dropNotNull(ColumnTransition column) {
    return List.of(new SqlServerAlterColumn(this, column));
  }

  @Override
  public List<Command> collate(ColumnTransition column, Collation collation) {
    return collation.appliesTo(ddl.dialect()) ? List.of(new SqlServerAlterColumn(this, column)) : List.of();
  }

  @Override
  public List<Command> setDefault(ColumnTransition column) {
    return altered(column) ? List.of(new SqlServerAlterColumn(this, column)) : replaceDefault(column);
  }

  @Override
  public List<Command> dropDefault(ColumnTransition column) {
    return altered(column) ? List.of(new SqlServerAlterColumn(this, column))
        : List.of(new SqlServerDropDefault(column.table().name(), column.name()));
  }

  @Override
  public List<Command> addIdentity(ColumnTransition column) {
    throw new UnsupportedChangeException("SQL Server cannot add IDENTITY to the existing column " + column.table().name() + "."
        + column.name() + ". Add a new column or a new table instead.");
  }

  @Override
  public List<Command> dropIdentity(ColumnTransition column) {
    throw new UnsupportedChangeException("SQL Server cannot remove IDENTITY from the existing column " + column.table().name()
        + "." + column.name() + ". Add a new column or a new table instead.");
  }

  List<Command> replaceDefault(ColumnTransition column) {
    Column target = column.definition();
    String value = target.attribute(Default.class).orElseThrow().sql(ddl.dialect(), target);
    return List.of(new SqlServerDropDefault(column.table().name(), column.name()), new AddDefault(column.table()
        .name(), new SqlServerDefaultName(column.table().definition(), target).value(), value, column.name()));
  }

  private boolean altered(ColumnTransition column) {
    String dialect = ddl.dialect().name();
    Column target = column.definition();
    return column.origin().filter(origin -> !origin.type().equals(target.type())
        || origin.nullable() != target.nullable()
        || !origin.collation(dialect).equals(target.collation(dialect))).isPresent();
  }

  private record AddColumn(TableName table, Clause definition) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" ADD ").clause(definition);
    }
  }

  private record AddDefault(TableName table, String name, String value, String column) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" ADD CONSTRAINT ").name(name)
          .append(" DEFAULT " + value + " FOR ").name(column);
    }
  }
}
