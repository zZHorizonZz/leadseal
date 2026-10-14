package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Node;
import dev.horizon.leadseal.Operation;

/** The MariaDB forms of the operations that MariaDB writes differently from standard SQL. */
sealed interface MariaDbOperation extends Node {

  /**
   * {@code ALTER TABLE ... RENAME TO}. MariaDB names every primary key {@code PRIMARY}, so the key keeps its name.
   *
   * @param operation the operation
   */
  record RenameTable(Operation.RenameTable operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(operation.from()).append(" RENAME TO ").name(operation.table());
    }
  }

  /**
   * {@code ALTER TABLE ... MODIFY COLUMN}, which sets the type and the nullability together.
   *
   * @param operation the operation
   */
  record AlterColumn(Operation.AlterColumn operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      if (operation.becomesNotNull()) {
        sql.append("UPDATE ").name(operation.table()).append(" SET ").name(operation.to().name())
            .append(" = " + sql.dialect().zero(operation.to()) + " WHERE ").name(operation.to().name())
            .append(" IS NULL").end();
      }
      sql.append("ALTER TABLE ").name(operation.table()).append(" MODIFY COLUMN ")
          .column(sql.tableAfter(operation.table()), operation.to());
      if (operation.to().nullable()) {
        sql.append(" NULL");
      }
    }
  }

  /**
   * {@code ALTER TABLE ... DROP PRIMARY KEY, ADD PRIMARY KEY}.
   *
   * @param operation the operation
   */
  record AlterPrimaryKey(Operation.AlterPrimaryKey operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(operation.table()).append(" DROP PRIMARY KEY, ADD PRIMARY KEY (")
          .names(operation.to().columns()).append(")");
    }
  }

  /**
   * {@code DROP INDEX ... ON}.
   *
   * @param operation the operation
   */
  record DropIndex(Operation.DropIndex operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("DROP INDEX ").name(operation.index().name()).append(" ON ").name(operation.table());
    }
  }

  /**
   * {@code ALTER TABLE ... DROP FOREIGN KEY}.
   *
   * @param operation the operation
   */
  record DropForeignKey(Operation.DropForeignKey operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(operation.table()).append(" DROP FOREIGN KEY ").name(operation.key().name());
    }
  }
}
