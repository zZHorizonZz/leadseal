package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

record SqlitePlan(SqliteDdl ddl, List<Change> changes, Transition transition) {

  List<Command> commands() {
    namespace().ifPresent(name -> {
      throw new UnsupportedChangeException("SQLite has no namespaces, so the table '" + name.name() + "' cannot be in the"
          + " namespace '" + name.namespace() + "'.");
    });
    List<List<Command>> each = changes.stream().map(change -> change.commands(ddl, transition)).toList();
    Set<String> rebuilt = rebuilt(each);
    boolean referenced = referenced(rebuilt);
    List<Command> all = new ArrayList<>();
    if (referenced) {
      all.add(new ForeignKeysOff());
    } else if (!rebuilt.isEmpty()) {
      all.add(new DeferForeignKeys());
    }
    for (int i = 0; i < changes.size(); i++) {
      if (!rebuilt.contains(changes.get(i).table(transition).id())) {
        all.addAll(each.get(i));
      }
    }
    transition.target().names().stream().map(name -> transition.target().table(name).orElseThrow().id())
        .filter(rebuilt::contains).forEach(id -> all.add(new SqliteRebuild(ddl, transition, id)));
    if (referenced) {
      all.add(new ForeignKeysOn());
    }
    return all.stream().distinct().toList();
  }

  private Optional<TableName> namespace() {
    return Stream.concat(transition.origin().names().stream(), transition.target().names().stream())
        .filter(name -> !name.namespace().equals(Namespace.Name.DEFAULT)).findFirst();
  }

  private Set<String> rebuilt(List<List<Command>> each) {
    Set<String> ids = new LinkedHashSet<>();
    for (int i = 0; i < changes.size(); i++) {
      String id = changes.get(i).table(transition).id();
      if (each.get(i).contains(new SqliteRebuild(ddl, transition, id))) {
        ids.add(id);
      }
    }
    return ids;
  }

  private boolean referenced(Set<String> rebuilt) {
    return transition.origin().names().stream().map(name -> transition.origin().table(name).orElseThrow())
        .flatMap(table -> table.attributes(ForeignKey.class).stream())
        .anyMatch(key -> transition.origin().table(key.table()).map(Table::id).filter(rebuilt::contains)
            .isPresent());
  }

  private record ForeignKeysOff() implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.outsideTransaction();
      script.append("PRAGMA foreign_keys = OFF").end();
      script.append("CREATE TEMP TABLE ").name(script.internal("check")).append(" (foreign_keys INTEGER CONSTRAINT"
          + " foreign_keys_must_be_off CHECK (foreign_keys = 0), violations INTEGER CONSTRAINT foreign_keys_must_hold"
          + " CHECK (violations = 0))").end();
      script.append("INSERT INTO ").name(script.internal("check"))
          .append(" (foreign_keys) SELECT foreign_keys FROM pragma_foreign_keys");
    }
  }

  private record ForeignKeysOn() implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("INSERT INTO ").name(script.internal("check"))
          .append(" (violations) SELECT COUNT(*) FROM pragma_foreign_key_check").end();
      script.append("DROP TABLE ").name(script.internal("check")).end();
      script.append("PRAGMA foreign_keys = ON");
    }
  }

  private record DeferForeignKeys() implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("PRAGMA defer_foreign_keys = ON");
    }
  }
}
