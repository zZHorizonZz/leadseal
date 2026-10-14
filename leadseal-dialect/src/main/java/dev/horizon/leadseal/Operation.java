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
   * @return the table name at the time the operation runs. Drops of tables, indexes, checks and foreign keys run before
   * renames and use the old name. All other operations run after renames and use the new name.
   */
  String table();

  /**
   * {@code CREATE TABLE} with its columns, primary key, checks, indexes and comments. Foreign keys are separate
   * {@link AddForeignKey} operations, which run after all tables exist. A table in a database schema creates the schema
   * first, if it does not exist.
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
      if (definition.schema() != null) {
        sql.render(new CreateSchema(definition.schema())).end();
      }
      sql.append("CREATE TABLE ").table(definition.name()).append(" (");
      for (Column column : definition.columns()) {
        sql.column(definition, column).append(", ");
      }
      sql.append("CONSTRAINT ").name(definition.primaryKey().name()).append(" PRIMARY KEY (")
          .names(definition.primaryKey().columns()).append(")");
      for (Check check : definition.checks()) {
        sql.append(", ");
        AddCheck.constraint(sql, check);
      }
      sql.append(")").end();
      for (Index index : definition.indexes()) {
        sql.render(new AddIndex(definition.name(), index)).end();
      }
      if (definition.comment() != null) {
        sql.render(new CommentTable(definition.name(), null, definition.comment())).end();
      }
      for (Column column : definition.columns()) {
        if (column.comment() != null) {
          sql.render(new ColumnComment(definition.name(), column.name(), null, column.comment())).end();
        }
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
      sql.append("DROP TABLE ").oldTable(table);
    }
  }

  /**
   * Renames a table and its primary key constraint. The table stays in its database schema.
   *
   * @param from the old table name
   * @param table the new table name
   */
  record RenameTable(String from, String table) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      String schema = sql.tableBefore(from).schema();
      sql.append("ALTER TABLE ").oldTable(from).append(" RENAME TO ").name(table).end();
      String oldKey = sql.tableBefore(from).primaryKey().name();
      String newKey = sql.tableAfter(table).primaryKey().name();
      if (!oldKey.equals(newKey)) {
        sql.append("ALTER TABLE ").qualified(schema, table).append(" RENAME CONSTRAINT ").name(oldKey).append(" TO ")
            .name(newKey);
      }
    }
  }

  /**
   * Moves a table to another database schema, and creates the schema if it does not exist.
   *
   * @param table the table name
   * @param from the schema before the move, or {@code null} for the default schema
   * @param to the schema after the move, or {@code null} for the default schema
   */
  record MoveTable(String table, String from, String to) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      if (to != null) {
        sql.render(new CreateSchema(to)).end();
        sql.append("ALTER TABLE ").qualified(from, table).append(" SET SCHEMA ").name(to);
        return;
      }
      String source = (from == null ? "" : sql.dialect().quote(from) + ".") + sql.dialect().quote(table);
      sql.append("DO $$ BEGIN EXECUTE format('ALTER TABLE " + source.replace("'", "''")
          + " SET SCHEMA %I', current_schema()); END $$");
    }
  }

  /**
   * Adds a column. A column that does not accept {@code NULL} and has no default is added with its
   * {@linkplain Dialect#zero zero value} as a default, which fills existing rows. That default is then removed.
   *
   * @param table the table name
   * @param column the new column
   */
  record AddColumn(String table, Column column) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").table(table).append(" ADD COLUMN ").column(sql.tableAfter(table), column);
      if (!column.nullable() && column.defaultValue() == null && !column.identity()) {
        sql.append(" DEFAULT " + sql.dialect().zero(column)).end();
        sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column.name()).append(" DROP DEFAULT");
      }
      sql.end();
      if (column.comment() != null) {
        sql.render(new ColumnComment(table, column.name(), null, column.comment()));
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
      sql.append("ALTER TABLE ").table(table).append(" DROP COLUMN ").name(column);
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
      sql.append("ALTER TABLE ").table(table).append(" RENAME COLUMN ").name(from).append(" TO ").name(to);
    }
  }

  /**
   * Changes a column: its type, length, precision, collation, nullability, default, identity or comment. When the
   * column stops accepting {@code NULL}, rows with {@code NULL} get the column's {@linkplain Dialect#fill default or
   * zero value} first.
   *
   * @param table the table name
   * @param from the column before the change
   * @param to the column after the change, with the same name
   */
  record AlterColumn(String table, Column from, Column to) implements Operation {

    /**
     * Returns whether the type, the length, the precision or the scale changes.
     *
     * @return {@code true} if the database type changes
     */
    public boolean typeChanged() {
      return from.type() != to.type() || !Objects.equals(from.length(), to.length())
          || !Objects.equals(from.precision(), to.precision()) || !Objects.equals(from.scale(), to.scale());
    }

    /**
     * Returns whether the collation of a dialect changes.
     *
     * @param dialect the dialect name
     * @return {@code true} if the collation changes
     */
    public boolean collationChanged(String dialect) {
      return !Objects.equals(from.collations().get(dialect), to.collations().get(dialect));
    }

    /**
     * Returns whether the column stops accepting {@code NULL}.
     *
     * @return {@code true} if the column becomes {@code NOT NULL}
     */
    public boolean becomesNotNull() {
      return from.nullable() && !to.nullable();
    }

    /**
     * Returns whether the column starts accepting {@code NULL}.
     *
     * @return {@code true} if the column becomes nullable
     */
    public boolean becomesNullable() {
      return !from.nullable() && to.nullable();
    }

    /**
     * Returns whether the default changes.
     *
     * @return {@code true} if the default is added, removed or changed
     */
    public boolean defaultChanged() {
      return !Objects.equals(from.defaultValue(), to.defaultValue());
    }

    /**
     * Returns whether the column becomes or stops being an identity column.
     *
     * @return {@code true} if the identity changes
     */
    public boolean identityChanged() {
      return from.identity() != to.identity();
    }

    /**
     * Returns whether the comment changes.
     *
     * @return {@code true} if the comment is added, removed or changed
     */
    public boolean commentChanged() {
      return !Objects.equals(from.comment(), to.comment());
    }

    @Override
    public void render(DdlWriter sql) {
      Table after = sql.tableAfter(table);
      String column = to.name();
      boolean retype = typeChanged() || collationChanged(sql.dialect().name());
      if (from.identity() && !to.identity()) {
        alter(sql).append(" DROP IDENTITY").end();
      }
      if (from.defaultValue() != null && (defaultChanged() || retype)) {
        alter(sql).append(" DROP DEFAULT").end();
      }
      if (retype) {
        String type = sql.dialect().type(after, to);
        alter(sql).append(" TYPE " + type);
        String collation = to.collations().get(sql.dialect().name());
        if (collation != null) {
          sql.append(" COLLATE ").name(collation);
        }
        sql.append(" USING ").name(column).append("::" + type).end();
      }
      if (becomesNotNull()) {
        sql.append("UPDATE ").table(table).append(" SET ").name(column).append(" = " + sql.dialect().fill(to)
            + " WHERE ").name(column).append(" IS NULL").end();
        alter(sql).append(" SET NOT NULL").end();
      } else if (becomesNullable()) {
        alter(sql).append(" DROP NOT NULL").end();
      }
      if (to.defaultValue() != null && (defaultChanged() || retype)) {
        alter(sql).append(" SET DEFAULT " + sql.dialect().defaultValue(to)).end();
      }
      if (!from.identity() && to.identity()) {
        alter(sql).append(" ADD GENERATED BY DEFAULT AS IDENTITY").end();
        String name = (after.schema() == null ? "" : sql.dialect().quote(after.schema()) + ".")
            + sql.dialect().quote(table);
        sql.append("SELECT setval(pg_get_serial_sequence(" + sql.dialect().string(name) + ", "
            + sql.dialect().string(column) + "), COALESCE(MAX(").name(column).append("), 0) + 1, false) FROM ")
            .table(table).end();
      }
      if (commentChanged()) {
        sql.render(new ColumnComment(table, column, from.comment(), to.comment())).end();
      }
    }

    private DdlWriter alter(DdlWriter sql) {
      return sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(to.name());
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
      sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(from.name()).end();
      sql.append("ALTER TABLE ").table(table).append(" ADD CONSTRAINT ").name(to.name()).append(" PRIMARY KEY (")
          .names(to.columns()).append(")");
    }
  }

  /**
   * {@code CREATE INDEX} or {@code CREATE UNIQUE INDEX}, with descending columns and a {@code WHERE} condition for a
   * partial index.
   *
   * @param table the table name
   * @param index the index
   */
  record AddIndex(String table, Index index) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append(index.unique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ").name(index.name()).append(" ON ")
          .table(table).append(" (");
      columns(sql, index);
      sql.append(")");
      if (index.where() != null) {
        sql.append(" WHERE " + index.where());
      }
    }

    /**
     * Renders the indexed columns, separated by commas, with {@code DESC} after each descending column.
     *
     * @param sql the writer
     * @param index the index
     */
    public static void columns(DdlWriter sql, Index index) {
      for (int i = 0; i < index.columns().size(); i++) {
        String column = index.columns().get(i);
        sql.append(i == 0 ? "" : ", ").name(column).append(index.descending().contains(column) ? " DESC" : "");
      }
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
      sql.append("DROP INDEX ").qualified(sql.tableBefore(table).schema(), index.name());
    }
  }

  /**
   * Adds a check constraint.
   *
   * @param table the table name
   * @param check the check
   */
  record AddCheck(String table, Check check) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").table(table).append(" ADD ");
      constraint(sql, check);
    }

    /**
     * Renders {@code CONSTRAINT name CHECK (expression)}.
     *
     * @param sql the writer
     * @param check the check
     */
    public static void constraint(DdlWriter sql, Check check) {
      sql.append("CONSTRAINT ").name(check.name()).append(" CHECK (" + check.expression() + ")");
    }
  }

  /**
   * Drops a check constraint.
   *
   * @param table the table name
   * @param check the check
   */
  record DropCheck(String table, Check check) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("ALTER TABLE ").oldTable(table).append(" DROP CONSTRAINT ").name(check.name());
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
      sql.append("ALTER TABLE ").table(table).append(" ADD ");
      constraint(sql, key);
    }

    /**
     * Renders {@code CONSTRAINT name FOREIGN KEY (...) REFERENCES table (...)} with its {@code ON DELETE} and
     * {@code ON UPDATE} actions.
     *
     * @param sql the writer
     * @param key the foreign key
     */
    public static void constraint(DdlWriter sql, ForeignKey key) {
      sql.append("CONSTRAINT ").name(key.name()).append(" FOREIGN KEY (").names(key.columns()).append(") REFERENCES ")
          .table(key.table()).append(" (").names(key.referencedColumns()).append(")")
          .append(action(" ON DELETE", key.onDelete())).append(action(" ON UPDATE", key.onUpdate()));
    }

    private static String action(String event, ForeignKey.Action action) {
      return switch (action) {
        case NO_ACTION -> "";
        case RESTRICT -> event + " RESTRICT";
        case CASCADE -> event + " CASCADE";
        case SET_NULL -> event + " SET NULL";
      };
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
      sql.append("ALTER TABLE ").oldTable(table).append(" DROP CONSTRAINT ").name(key.name());
    }
  }

  /**
   * Sets or removes the comment of a table: {@code COMMENT ON TABLE}.
   *
   * @param table the table name
   * @param from the comment before, or {@code null}
   * @param to the comment after, or {@code null} to remove it
   */
  record CommentTable(String table, String from, String to) implements Operation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("COMMENT ON TABLE ").table(table).append(" IS " + (to == null ? "NULL" : sql.dialect().string(to)));
    }
  }
}
