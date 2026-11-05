package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.AddConstraint;
import dev.horizon.leadseal.command.Ddl;
import java.util.ArrayList;
import java.util.List;

record SqlServerDependents(Ddl ddl, ColumnTransition column) {

  List<Command> drops() {
    List<Command> all = new ArrayList<>();
    keys().forEach(key -> all.addAll(ddl.constraint().dropForeignKey(key.table(), key.key())));
    indexes().forEach(index -> all.addAll(ddl.constraint().dropIndex(column.table(), index)));
    checks().forEach(check -> all.addAll(ddl.constraint().dropCheck(column.table(), check)));
    if (keyed()) {
      all.add(new DropPrimaryKey(column.table().name()));
    }
    return all;
  }

  List<Command> primaryKey() {
    return keyed() ? List.of(new AddConstraint(column.table().name(), new Clause.PrimaryKeyConstraint(target()
        .primaryKey()))) : List.of();
  }

  List<Command> creates() {
    List<Command> all = new ArrayList<>();
    checks().forEach(check -> all.addAll(ddl.constraint().addCheck(column.table(), check)));
    indexes().forEach(index -> all.addAll(ddl.constraint().addIndex(column.table(), index)));
    keys().forEach(key -> all.addAll(ddl.constraint().addForeignKey(key.table(), key.key())));
    return all;
  }

  private Table origin() {
    return column.table().origin().orElseThrow();
  }

  private Table target() {
    return column.table().definition();
  }

  private boolean keyed() {
    return origin().primaryKey().columns().contains(column.origin().orElseThrow().name());
  }

  private List<Index> indexes() {
    List<TableAttribute> originAttributes = originAttributes(column.table());
    return target().attributes(Index.class).stream()
        .filter(index -> index.contains(column.name()) && originAttributes.contains(index))
        .toList();
  }

  private List<Check> checks() {
    List<TableAttribute> originAttributes = originAttributes(column.table());
    return target().attributes(Check.class).stream().filter(originAttributes::contains).toList();
  }

  private List<TableAttribute> originAttributes(TableTransition table) {
    return table.origin().map(origin -> origin.attributes().stream()
        .flatMap(attribute -> attribute.rename(table.originName(), table.transition()).stream()).toList())
        .orElse(List.of());
  }

  private List<Key> keys() {
    Transition transition = column.table().transition();
    TableName owner = column.table().name();
    List<Key> all = new ArrayList<>();
    for (Namespace namespace : transition.target().namespaces()) {
      for (Table definition : namespace.tables()) {
        TableName name = namespace.nameOf(definition);
        TableTransition table = new TableTransition(transition, name, definition.id());
        List<TableAttribute> originAttributes = originAttributes(table);
        definition.attributes(ForeignKey.class).stream()
            .filter(key -> originAttributes.contains(key) && (name.equals(owner) && key.columns().contains(column.name())
                || key.table().equals(owner) && key.referencedColumns().contains(column.name())))
            .forEach(key -> all.add(new Key(table, key)));
      }
    }
    return all;
  }

  private record Key(TableTransition table, ForeignKey key) {
  }

  private record DropPrimaryKey(TableName table) implements Command {

    @Override
    public void render(ScriptWriter script) {
      Dialect dialect = script.dialect();
      String path = dialect.quote(table);
      String statement = SqlServerVariable.of(script, "primary_key").name();
      script.append("DECLARE " + statement + " nvarchar(max) = (SELECT "
          + dialect.string("ALTER TABLE " + path + " DROP CONSTRAINT ") + " + QUOTENAME(name) FROM"
          + " sys.key_constraints WHERE type = 'PK' AND parent_object_id = OBJECT_ID(" + dialect.string(path)
          + ")) IF " + statement + " IS NOT NULL EXEC(" + statement + ")");
    }
  }
}
