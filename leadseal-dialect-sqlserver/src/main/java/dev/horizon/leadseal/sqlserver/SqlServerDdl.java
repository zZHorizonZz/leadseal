package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.CommentDdl;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;

record SqlServerDdl(Dialect dialect) implements Ddl {

  @Override
  public TableDdl table() {
    return new SqlServerTableDdl(this);
  }

  @Override
  public ColumnDdl column() {
    return new SqlServerColumnDdl(this);
  }

  @Override
  public ConstraintDdl constraint() {
    return new SqlServerConstraintDdl(this);
  }

  @Override
  public CommentDdl comment() {
    return new SqlServerCommentDdl(dialect);
  }
}
