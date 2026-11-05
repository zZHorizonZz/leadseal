package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.CommentDdl;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;
import dev.horizon.leadseal.command.TypeDdl;

record PostgresDdl(Dialect dialect) implements Ddl {

  @Override
  public TableDdl table() {
    return new PostgresTableDdl(this);
  }

  @Override
  public ColumnDdl column() {
    return new PostgresColumnDdl(this);
  }

  @Override
  public ConstraintDdl constraint() {
    return () -> this;
  }

  @Override
  public TypeDdl type() {
    return new PostgresTypeDdl(this);
  }

  @Override
  public CommentDdl comment() {
    return new PostgresCommentDdl(dialect);
  }
}
