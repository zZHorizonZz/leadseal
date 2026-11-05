package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.TableChange;
import dev.horizon.leadseal.change.TypeChange;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

record SqlitePlan(SqliteDdl ddl, List<Change> changes, Transition transition) implements Plan {

  @Override
  public List<Command> commands() {
    List<Command> typeCommands = new ArrayList<>();
    List<ChangeCommands> changeCommands = new ArrayList<>();
    for (Change change : changes) {
      switch (change) {
        case TableChange table -> changeCommands.add(new ChangeCommands(new SqliteRebuild(ddl, transition,
            table.table(transition).id()), table.commands(ddl, transition)));
        case TypeChange type -> typeCommands.addAll(type.commands(ddl, transition));
      }
    }
    Set<SqliteRebuild> tableRebuilds = changeCommands.stream().filter(ChangeCommands::needsRebuild)
        .map(ChangeCommands::tableRebuild).collect(Collectors.toSet());
    ForeignKeyMode mode = foreignKeyMode(tableRebuilds);
    List<Command> all = new ArrayList<>(mode.start());
    all.addAll(typeCommands);
    changeCommands.stream().filter(change -> !tableRebuilds.contains(change.tableRebuild()))
        .forEach(change -> all.addAll(change.commands()));
    transition.target().names().stream()
        .map(name -> new SqliteRebuild(ddl, transition, transition.target().table(name).orElseThrow().id()))
        .filter(tableRebuilds::contains).forEach(all::add);
    all.addAll(mode.end());
    return merge(all);
  }

  private ForeignKeyMode foreignKeyMode(Set<SqliteRebuild> tableRebuilds) {
    if (tableRebuilds.isEmpty()) {
      return new Immediate();
    }
    Set<String> tables = tableRebuilds.stream().map(SqliteRebuild::id).collect(Collectors.toSet());
    boolean references = transition.origin().names().stream().map(name -> transition.origin().table(name).orElseThrow())
        .flatMap(table -> table.attributes(ForeignKey.class).stream())
        .anyMatch(key -> transition.origin().table(key.table()).map(Table::id).filter(tables::contains).isPresent());
    return references ? new Suspension() : new Deferral();
  }

  private record ChangeCommands(SqliteRebuild tableRebuild, List<Command> commands) {

    boolean needsRebuild() {
      return commands.contains(tableRebuild);
    }
  }

  private sealed interface ForeignKeyMode permits Immediate, Deferral, Suspension {

    List<Command> start();

    default List<Command> end() {
      return List.of();
    }
  }

  private record Immediate() implements ForeignKeyMode {

    @Override
    public List<Command> start() {
      return List.of();
    }
  }

  private record Deferral() implements ForeignKeyMode {

    @Override
    public List<Command> start() {
      return List.of(new DeferForeignKeys());
    }
  }

  private record Suspension() implements ForeignKeyMode {

    @Override
    public List<Command> start() {
      return List.of(new ForeignKeysOff());
    }

    @Override
    public List<Command> end() {
      return List.of(new ForeignKeysOn());
    }
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
