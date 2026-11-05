package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.ConstraintDdl;
import java.util.List;
import java.util.function.Supplier;

record SqliteConstraintDdl(SqliteDdl ddl) implements ConstraintDdl {

  @Override
  public List<Command> alterPrimaryKey(TableTransition table, PrimaryKey origin, PrimaryKey target) {
    return rebuild(table);
  }

  @Override
  public List<Command> addIndex(TableTransition table, Index index) {
    return unlessCreated(table, () -> ConstraintDdl.super.addIndex(table, index));
  }

  @Override
  public List<Command> dropIndex(TableTransition table, Index index) {
    return unlessDropped(table, () -> ConstraintDdl.super.dropIndex(table, index));
  }

  @Override
  public List<Command> addCheck(TableTransition table, Check check) {
    return unlessCreated(table, () -> rebuild(table));
  }

  @Override
  public List<Command> dropCheck(TableTransition table, Check check) {
    return unlessDropped(table, () -> rebuild(table));
  }

  @Override
  public List<Command> addForeignKey(TableTransition table, ForeignKey key) {
    return unlessCreated(table, () -> rebuild(table));
  }

  @Override
  public List<Command> dropForeignKey(TableTransition table, ForeignKey key) {
    return unlessDropped(table, () -> rebuild(table));
  }

  private List<Command> rebuild(TableTransition table) {
    return List.of(new SqliteRebuild(ddl, table.transition(), table.id()));
  }

  private List<Command> unlessCreated(TableTransition table, Supplier<List<Command>> commands) {
    return table.origin().isPresent() ? commands.get() : List.of();
  }

  private List<Command> unlessDropped(TableTransition table, Supplier<List<Command>> commands) {
    return table.target().isPresent() ? commands.get() : List.of();
  }
}
