package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.Node;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.Table;
import java.util.List;

/** The SQL Server forms of the operations that SQL Server writes differently from standard SQL. */
sealed interface SqlServerOperation extends Node {

  /**
   * Renames a table and its primary key constraint with {@code sp_rename}.
   *
   * @param operation the operation
   */
  record RenameTable(Operation.RenameTable operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("EXEC sp_rename " + SqlServerDialect.literal(operation.from()) + ", "
          + SqlServerDialect.literal(operation.table())).end();
      String oldKey = sql.tableBefore(operation.from()).primaryKey().name();
      String newKey = sql.tableAfter(operation.table()).primaryKey().name();
      if (!oldKey.equals(newKey)) {
        sql.append("EXEC sp_rename " + SqlServerDialect.literal(oldKey) + ", " + SqlServerDialect.literal(newKey)
            + ", N'OBJECT'");
      }
    }
  }

  /**
   * Renames a column with {@code sp_rename}.
   *
   * @param operation the operation
   */
  record RenameColumn(Operation.RenameColumn operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("EXEC sp_rename " + SqlServerDialect.literal(operation.table() + "." + operation.from()) + ", "
          + SqlServerDialect.literal(operation.to()) + ", N'COLUMN'");
    }
  }

  /**
   * {@code ALTER TABLE ... ADD}. A {@code NOT NULL} column gets a named default constraint that fills existing rows,
   * which is then dropped.
   *
   * @param operation the operation
   */
  record AddColumn(Operation.AddColumn operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      Column column = operation.column();
      sql.append("ALTER TABLE ").name(operation.table()).append(" ADD ").column(sql.tableAfter(operation.table()),
          column);
      if (column.nullable()) {
        return;
      }
      String constraint = "df_" + operation.table() + "_" + column.name();
      sql.append(" CONSTRAINT ").name(constraint).append(" DEFAULT " + sql.dialect().zero(column)).end();
      sql.append("ALTER TABLE ").name(operation.table()).append(" DROP CONSTRAINT ").name(constraint);
    }
  }

  /**
   * {@code ALTER TABLE ... ALTER COLUMN}. SQL Server cannot change a column while an index, the primary key or a
   * foreign key of its table uses it. They are dropped before the change and created again after it.
   *
   * @param operation the operation
   */
  record AlterColumn(Operation.AlterColumn operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      String table = operation.table();
      String column = operation.to().name();
      Table after = sql.tableAfter(table);
      Table before = sql.before().tableById(after.id()).orElseThrow();
      List<Index> indexes = after.indexes().stream().filter(index -> index.columns().contains(column))
          .filter(index -> before.indexes().stream().anyMatch(was -> was.name().equals(index.name()))).toList();
      List<ForeignKey> keys = after.foreignKeys().stream().filter(key -> key.columns().contains(column))
          .filter(key -> before.foreignKeys().stream().anyMatch(was -> was.name().equals(key.name()))).toList();
      boolean primaryKey = after.primaryKey().columns().contains(column)
          && before.primaryKey().name().equals(after.primaryKey().name())
          && before.primaryKey().columns().size() == after.primaryKey().columns().size();
      for (ForeignKey key : keys) {
        sql.render(new Operation.DropForeignKey(table, key)).end();
      }
      for (Index index : indexes) {
        sql.render(new Operation.DropIndex(table, index)).end();
      }
      if (primaryKey) {
        sql.append("ALTER TABLE ").name(table).append(" DROP CONSTRAINT ").name(after.primaryKey().name()).end();
      }
      if (operation.becomesNotNull()) {
        sql.append("UPDATE ").name(table).append(" SET ").name(column).append(" = " + sql.dialect().zero(operation.to())
            + " WHERE ").name(column).append(" IS NULL").end();
      }
      sql.append("ALTER TABLE ").name(table).append(" ALTER COLUMN ").column(after, operation.to())
          .append(operation.to().nullable() ? " NULL" : "").end();
      if (primaryKey) {
        sql.append("ALTER TABLE ").name(table).append(" ADD CONSTRAINT ").name(after.primaryKey().name())
            .append(" PRIMARY KEY (").names(after.primaryKey().columns()).append(")").end();
      }
      for (Index index : indexes) {
        sql.render(new Operation.AddIndex(table, index)).end();
      }
      for (ForeignKey key : keys) {
        sql.render(new Operation.AddForeignKey(table, key)).end();
      }
    }
  }

  /**
   * {@code DROP INDEX ... ON}.
   *
   * @param operation the operation
   */
  record DropIndex(Operation.DropIndex operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("DROP INDEX ").name(operation.index().name()).append(" ON ").name(operation.table());
    }
  }

  /**
   * Adds a foreign key. SQL Server has no {@code ON DELETE RESTRICT}. A restricting foreign key uses the default,
   * {@code NO ACTION}, which also rejects the delete.
   *
   * @param operation the operation
   */
  record AddForeignKey(Operation.AddForeignKey operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      ForeignKey key = operation.key();
      ForeignKey supported = key.onDelete() != ForeignKey.Action.RESTRICT ? key
          : new ForeignKey(key.name(), key.columns(), key.table(), key.referencedColumns(),
              ForeignKey.Action.NO_ACTION);
      sql.append("ALTER TABLE ").name(operation.table()).append(" ADD ");
      Operation.AddForeignKey.constraint(sql, supported);
    }
  }
}
