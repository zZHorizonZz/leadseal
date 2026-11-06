package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.command.CommentDdl;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;
import dev.horizon.leadseal.command.TriggerDdl;

record MariaDbDdl(Dialect dialect) implements Ddl {

  @Override
  public TableDdl table() {
    return new MariaDbTableDdl(this);
  }

  @Override
  public MariaDbColumnDdl column() {
    return new MariaDbColumnDdl(this);
  }

  @Override
  public ConstraintDdl constraint() {
    return new MariaDbConstraintDdl(this);
  }

  @Override
  public TriggerDdl trigger() {
    return new MariaDbTriggerDdl(this);
  }

  @Override
  public CommentDdl comment() {
    return new MariaDbCommentDdl(column());
  }
}
