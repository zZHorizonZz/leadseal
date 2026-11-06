package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.CommentDdl;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.Plan;
import dev.horizon.leadseal.command.TriggerDdl;
import java.util.List;

record SqliteDdl(Dialect dialect) implements Ddl {

  @Override
  public Plan plan(List<Change> changes, Transition transition) {
    return new SqlitePlan(this, changes, transition);
  }

  @Override
  public SqliteTableDdl table() {
    return new SqliteTableDdl(this);
  }

  @Override
  public SqliteColumnDdl column() {
    return new SqliteColumnDdl(this);
  }

  @Override
  public ConstraintDdl constraint() {
    return new SqliteConstraintDdl(this);
  }

  @Override
  public TriggerDdl trigger() {
    return new SqliteTriggerDdl(this);
  }

  @Override
  public CommentDdl comment() {
    return new SqliteCommentDdl();
  }
}
