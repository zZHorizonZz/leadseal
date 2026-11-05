package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.AlterColumn;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.EnumType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

record PostgresColumnDdl(Ddl ddl) implements ColumnDdl {

  @Override
  public List<Command> alterColumnType(ColumnTransition column) {
    if (ddl.dialect().type(column.table().origin().orElseThrow(), column.origin().orElseThrow()).equals(type(column))) {
      return List.of();
    }
    Optional<Default> value = column.definition().attribute(Default.class);
    List<Command> all = new ArrayList<>();
    value.ifPresent(_ -> all.addAll(dropDefault(column)));
    String type = type(column);
    String text = column.origin().orElseThrow().type() instanceof EnumType(_, _, EnumType.Storage storage)
        ? storage.select("::text", "")
        : "";
    all.add(new AlterColumn(column.table().name(), column.name(), "TYPE " + type + collation(column) + " USING "
        + ddl.dialect().quote(column.name()) + text + "::" + type));
    value.ifPresent(_ -> all.addAll(setDefault(column)));
    return all;
  }

  @Override
  public List<Command> addIdentity(ColumnTransition column) {
    List<Command> all = new ArrayList<>(ColumnDdl.super.addIdentity(column));
    all.add(new IdentityRestart(column.table().name(), column.name()));
    return all;
  }

  @Override
  public List<Command> collate(ColumnTransition column, Collation collation) {
    if (!collation.appliesTo(ddl.dialect())) {
      return List.of();
    }
    return List.of(new AlterColumn(column.table().name(), column.name(), "TYPE " + type(column)
        + collation(column)));
  }

  private String type(ColumnTransition column) {
    return ddl.dialect().type(column.table().definition(), column.definition());
  }

  private String collation(ColumnTransition column) {
    Dialect dialect = ddl.dialect();
    return column.definition().collation(dialect.name()).map(name -> " COLLATE " + dialect.quote(name)).orElse("");
  }

  private record IdentityRestart(TableName table, String column) implements Command {

    @Override
    public void render(ScriptWriter script) {
      Dialect dialect = script.dialect();
      script.append("SELECT setval(pg_get_serial_sequence(" + dialect.string(dialect.quote(table)) + ", "
          + dialect.string(column) + "), COALESCE(MAX(").name(column).append("), 0) + 1, false) FROM ").table(table);
    }
  }
}
