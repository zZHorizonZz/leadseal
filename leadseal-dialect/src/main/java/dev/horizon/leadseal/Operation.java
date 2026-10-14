package dev.horizon.leadseal;

import java.util.Objects;

/**
 * One change of a schema. A migration's {@code ops.json} lists its operations, and each dialect renders them into SQL.
 *
 * <p>
 * The records in this interface render standard SQL. Operations refer to tables by name.
 */
public sealed interface Operation extends Node {

  /**
   * The table that the operation changes.
   *
   * @return the table name at the time the operation runs. Drops of tables, indexes and foreign keys run before
   * renames and use the old name. All other operations run after renames and use the new name.
   */
  String table();

  /**
   * {@code CREATE TABLE} with its columns, primary key and indexes. Foreign keys are separate
   * {@link AddForeignKey} operations, which run after all tables exist.
   *
   * @param definition the table
   */
  record CreateTable(Table definition) implements Operation {

    @Override
    public String table() {
      return definition.name();
    }

    @Override
    public void render(DdlWriter sql) {
      sql.append("CREATE TABLE ").name(definition.name()).append(" (");
      for (Column column : definition.columns()) {
        sql.column(definition, column).append(", ");
      }
      sql.append("CONSTRAINT ").name(definition.primaryKey().name()).append(" PRIMARY KEY (")
          .names(definition.primaryKey().columns()).append("))").end();
      for (Index index : definition.indexes()) {
        sql.render(new AddIndex(definition.name(), index)).end();
      }
    }
  }

  /**
   * {@code DROP TABLE}.
   *
   * @param table the table name
   */
  record DropTable(String table) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("DROP TABLE ").name(table);
    }
  }

  /**
   * Renames a table and its primary key constraint.
   *
   * @param from the old table name
   * @param table the new table name
   */
  record RenameTable(String from, String table) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(from).append(" RENAME TO ").name(table).end();
      String oldKey = sql.tableBefore(from).primaryKey().name();
      String newKey = sql.tableAfter(table).primaryKey().name();
      if (!oldKey.equals(newKey)) {
        sql.append("ALTER TABLE ").name(table).append(" RENAME CONSTRAINT ").name(oldKey).append(" TO ")
            .name(newKey);
      }
    }
  }

  /**
   * Adds a column. A column that does not accept {@code NULL} is added with its {@linkplain Dialect#zero zero value}
   * as a default, which fills existing rows. The default is then removed.
   *
   * @param table the table name
   * @param column the new column
   */
  record AddColumn(String table, Column column) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(table).append(" ADD COLUMN ").column(sql.tableAfter(table), column);
      if (!column.nullable()) {
        sql.append(" DEFAULT " + sql.dialect().zero(column)).end();
        sql.append("ALTER TABLE ").name(table).append(" ALTER COLUMN ").name(column.name()).append(" DROP DEFAULT");
      }
    }
  }

  /**
   * Drops a column.
   *
   * @param table the table name
   * @param column the column name
   */
  record DropColumn(String table, String column) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(table).append(" DROP COLUMN ").name(column);
    }
  }

  /**
   * Renames a column and keeps its data.
   *
   * @param table the table name
   * @param from the old column name
   * @param to the new column name
   */
  record RenameColumn(String table, String from, String to) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(table).append(" RENAME COLUMN ").name(from).append(" TO ").name(to);
    }
  }

  /**
   * Changes the type, the length or the nullability of a column. When the column stops accepting {@code NULL}, rows
   * with {@code NULL} get the column's {@linkplain Dialect#zero zero value} first.
   *
   * @param table the table name
   * @param from the column before the change
   * @param to the column after the change, with the same name
   */
  record AlterColumn(String table, Column from, Column to) implements Operation {

    /**
     * Returns whether the type or the length changes.
     *
     * @return {@code true} if the type or the length changes
     */
    public boolean typeChanged() {
      return from.type() != to.type() || !Objects.equals(from.length(), to.length());
    }

    /**
     * Returns whether the column stops accepting {@code NULL}.
     *
     * @return {@code true} if the column becomes {@code NOT NULL}
     */
    public boolean becomesNotNull() {
      return from.nullable() && !to.nullable();
    }

    @Override
    public void render(DdlWriter sql) {
      Table after = sql.tableAfter(table);
      if (typeChanged()) {
        String type = sql.dialect().type(after, to);
        sql.append("ALTER TABLE ").name(table).append(" ALTER COLUMN ").name(to.name()).append(" TYPE " + type
            + " USING ").name(to.name()).append("::" + type).end();
      }
      if (becomesNotNull()) {
        sql.append("UPDATE ").name(table).append(" SET ").name(to.name()).append(" = " + sql.dialect().zero(to)
            + " WHERE ").name(to.name()).append(" IS NULL").end();
        sql.append("ALTER TABLE ").name(table).append(" ALTER COLUMN ").name(to.name()).append(" SET NOT NULL");
      } else if (!from.nullable() && to.nullable()) {
        sql.append("ALTER TABLE ").name(table).append(" ALTER COLUMN ").name(to.name()).append(" DROP NOT NULL");
      }
    }
  }

  /**
   * Replaces the primary key.
   *
   * @param table the table name
   * @param from the primary key before the change
   * @param to the primary key after the change
   */
  record AlterPrimaryKey(String table, PrimaryKey from, PrimaryKey to) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(table).append(" DROP CONSTRAINT ").name(from.name()).end();
      sql.append("ALTER TABLE ").name(table).append(" ADD CONSTRAINT ").name(to.name()).append(" PRIMARY KEY (")
          .names(to.columns()).append(")");
    }
  }

  /**
   * {@code CREATE INDEX} or {@code CREATE UNIQUE INDEX}.
   *
   * @param table the table name
   * @param index the index
   */
  record AddIndex(String table, Index index) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append(index.unique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ").name(index.name()).append(" ON ")
          .name(table).append(" (").names(index.columns()).append(")");
    }
  }

  /**
   * {@code DROP INDEX}.
   *
   * @param table the table name
   * @param index the index
   */
  record DropIndex(String table, Index index) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("DROP INDEX ").name(index.name());
    }
  }

  /**
   * Adds a foreign key constraint.
   *
   * @param table the table name
   * @param key the foreign key
   */
  record AddForeignKey(String table, ForeignKey key) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(table).append(" ADD ");
      constraint(sql, key);
    }

    /**
     * Renders {@code CONSTRAINT name FOREIGN KEY (...) REFERENCES table (...) ON DELETE ...}.
     *
     * @param sql the writer
     * @param key the foreign key
     */
    public static void constraint(DdlWriter sql, ForeignKey key) {
      sql.append("CONSTRAINT ").name(key.name()).append(" FOREIGN KEY (").names(key.columns()).append(") REFERENCES ")
          .name(key.table()).append(" (").names(key.referencedColumns()).append(")");
      switch (key.onDelete()) {
        case NO_ACTION -> {
        }
        case RESTRICT -> sql.append(" ON DELETE RESTRICT");
        case CASCADE -> sql.append(" ON DELETE CASCADE");
        case SET_NULL -> sql.append(" ON DELETE SET NULL");
      }
    }
  }

  /**
   * Drops a foreign key constraint.
   *
   * @param table the table name
   * @param key the foreign key
   */
  record DropForeignKey(String table, ForeignKey key) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").name(table).append(" DROP CONSTRAINT ").name(key.name());
    }
  }
}
