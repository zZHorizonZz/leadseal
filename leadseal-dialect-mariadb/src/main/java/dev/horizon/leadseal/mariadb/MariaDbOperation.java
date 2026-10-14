package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnDefinition;
import dev.horizon.leadseal.CreateSchema;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Node;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;

/** The MariaDB forms of the nodes that MariaDB writes differently from standard SQL. */
sealed interface MariaDbOperation extends Node {

  /** A node without SQL, for column comments, which MariaDB writes in the column definition. */
  Node NOTHING = sql -> {
  };

  /**
   * A column definition: the name, the type, the collation, {@code NOT NULL}, the default, {@code AUTO_INCREMENT} and
   * the comment.
   *
   * @param definition the definition
   */
  record Definition(ColumnDefinition definition) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      Column column = definition.column();
      sql.name(column.name()).append(" " + sql.dialect().type(definition.table(), column));
      String collation = column.collations().get(sql.dialect().name());
      if (collation != null) {
        sql.append(" COLLATE " + MariaDbDialect.collation(collation));
      }
      sql.append(column.nullable() ? " NULL" : " NOT NULL");
      if (column.defaultValue() != null) {
        sql.append(" DEFAULT " + sql.dialect().defaultValue(column));
      }
      if (column.identity()) {
        sql.append(" AUTO_INCREMENT");
      }
      if (column.comment() != null) {
        sql.append(" COMMENT " + sql.dialect().string(column.comment()));
      }
    }
  }

  /**
   * {@code ALTER TABLE ... RENAME TO}, with the new name in the same database. MariaDB names every primary key
   * {@code PRIMARY}, so the key keeps its name.
   *
   * @param operation the operation
   */
  record RenameTable(Operation.RenameTable operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").oldTable(operation.from()).append(" RENAME TO ")
          .qualified(sql.tableBefore(operation.from()).schema(), operation.table());
    }
  }

  /**
   * {@code RENAME TABLE} into another database, which is created first if it does not exist.
   *
   * @param operation the operation
   */
  record MoveTable(Operation.MoveTable operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      if (operation.to() != null) {
        sql.render(new CreateSchema(operation.to())).end();
      }
      sql.append("RENAME TABLE ").qualified(operation.from(), operation.table()).append(" TO ")
          .qualified(operation.to(), operation.table());
    }
  }

  /**
   * {@code ALTER TABLE ... MODIFY COLUMN}, which sets the whole column definition at once.
   *
   * @param operation the operation
   */
  record AlterColumn(Operation.AlterColumn operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      Column to = operation.to();
      if (operation.becomesNotNull()) {
        sql.append("UPDATE ").table(operation.table()).append(" SET ").name(to.name())
            .append(" = " + sql.dialect().fill(to) + " WHERE ").name(to.name()).append(" IS NULL").end();
      }
      sql.append("ALTER TABLE ").table(operation.table()).append(" MODIFY COLUMN ")
          .column(sql.tableAfter(operation.table()), to);
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
      sql.append("ALTER TABLE ").table(operation.table()).append(" DROP PRIMARY KEY, ADD PRIMARY KEY (")
          .names(operation.to().columns()).append(")");
    }
  }

  /**
   * {@code CREATE INDEX}. MariaDB has no partial indexes.
   *
   * @param operation the operation
   */
  record AddIndex(Operation.AddIndex operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      if (operation.index().where() != null) {
        throw new SchemaException("MariaDB has no partial indexes, so it cannot create the index '"
            + operation.index().name() + "' with the condition " + operation.index().where() + ".");
      }
      operation.render(sql);
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
      sql.append("DROP INDEX ").name(operation.index().name()).append(" ON ").oldTable(operation.table());
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
      sql.append("ALTER TABLE ").oldTable(operation.table()).append(" DROP FOREIGN KEY ")
          .name(operation.key().name());
    }
  }

  /**
   * {@code ALTER TABLE ... COMMENT}.
   *
   * @param operation the operation
   */
  record CommentTable(Operation.CommentTable operation) implements MariaDbOperation {

    @Override
    public void render(DdlWriter sql) {
      String comment = operation.to() == null ? "" : operation.to();
      sql.append("ALTER TABLE ").table(operation.table()).append(" COMMENT = " + sql.dialect().string(comment));
    }
  }
}
