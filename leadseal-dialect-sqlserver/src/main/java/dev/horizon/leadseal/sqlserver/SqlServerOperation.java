package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Check;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnComment;
import dev.horizon.leadseal.ColumnDefinition;
import dev.horizon.leadseal.CreateSchema;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.Node;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.ArrayList;
import java.util.List;

/** The SQL Server forms of the nodes that SQL Server writes differently from standard SQL. */
sealed interface SqlServerOperation extends Node {

  /**
   * A column definition: the name, the type, the collation, {@code IDENTITY}, {@code NOT NULL} and a named default
   * constraint.
   *
   * @param definition the definition
   */
  record Definition(ColumnDefinition definition) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      Column column = definition.column();
      type(sql, definition.table(), column);
      if (column.defaultValue() != null) {
        sql.append(" CONSTRAINT ").name(Default.constraintName(definition.table().name(), column.name()))
            .append(" DEFAULT " + sql.dialect().defaultValue(column));
      }
    }

    /** Renders the name, the type, the collation, {@code IDENTITY} and {@code NULL} or {@code NOT NULL}. */
    static void type(DdlWriter sql, Table table, Column column) {
      sql.name(column.name()).append(" " + sql.dialect().type(table, column));
      String collation = column.collations().get(sql.dialect().name());
      if (collation != null) {
        sql.append(" COLLATE " + SqlServerDialect.collation(collation));
      }
      if (column.identity()) {
        sql.append(" IDENTITY(1, 1)");
      }
      sql.append(column.nullable() ? " NULL" : " NOT NULL");
    }
  }

  /**
   * Creates a schema if it does not exist. SQL Server has no {@code CREATE SCHEMA IF NOT EXISTS}.
   *
   * @param node the node
   */
  record CreateSchemaIfMissing(CreateSchema node) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      sql.append("IF SCHEMA_ID(" + sql.dialect().string(node.schema()) + ") IS NULL EXEC("
          + sql.dialect().string("CREATE SCHEMA " + sql.dialect().quote(node.schema())) + ")");
    }
  }

  /**
   * Renames a table and its primary key constraint with {@code sp_rename}.
   *
   * @param operation the operation
   */
  record RenameTable(Operation.RenameTable operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      String schema = sql.tableBefore(operation.from()).schema();
      sql.append("EXEC sp_rename " + name(sql, schema, operation.from()) + ", "
          + sql.dialect().string(operation.table())).end();
      String oldKey = sql.tableBefore(operation.from()).primaryKey().name();
      String newKey = sql.tableAfter(operation.table()).primaryKey().name();
      if (!oldKey.equals(newKey)) {
        sql.append("EXEC sp_rename " + name(sql, schema, oldKey) + ", " + sql.dialect().string(newKey)
            + ", N'OBJECT'");
      }
    }
  }

  /**
   * Moves a table to another schema with {@code ALTER SCHEMA ... TRANSFER}.
   *
   * @param operation the operation
   */
  record MoveTable(Operation.MoveTable operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      if (operation.to() != null) {
        sql.render(new CreateSchema(operation.to())).end();
        sql.append("ALTER SCHEMA ").name(operation.to()).append(" TRANSFER ")
            .qualified(operation.from(), operation.table());
        return;
      }
      String move = variable("move", operation.table());
      sql.append("DECLARE " + move + " nvarchar(max) = N'ALTER SCHEMA ' + QUOTENAME(SCHEMA_NAME()) + N' TRANSFER ' + "
          + name(sql, operation.from(), operation.table()) + " EXEC(" + move + ")");
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
      Table table = sql.tableAfter(operation.table());
      String column = qualified(sql, table.schema(), operation.table()) + "." + sql.dialect().quote(operation.from());
      sql.append("EXEC sp_rename " + sql.dialect().string(column) + ", " + sql.dialect().string(operation.to())
          + ", N'COLUMN'");
    }
  }

  /**
   * {@code ALTER TABLE ... ADD}. A {@code NOT NULL} column without a default gets a default constraint with its zero
   * value that fills existing rows, which is then dropped.
   *
   * @param operation the operation
   */
  record AddColumn(Operation.AddColumn operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      Column column = operation.column();
      String table = operation.table();
      sql.append("ALTER TABLE ").table(table).append(" ADD ").column(sql.tableAfter(table), column);
      if (!column.nullable() && column.defaultValue() == null && !column.identity()) {
        String constraint = Default.constraintName(table, column.name());
        sql.append(" CONSTRAINT ").name(constraint).append(" DEFAULT " + sql.dialect().zero(column)).end();
        sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(constraint);
      }
      sql.end();
      if (column.comment() != null) {
        sql.render(new ColumnComment(table, column.name(), null, column.comment()));
      }
    }
  }

  /**
   * {@code ALTER TABLE ... DROP COLUMN}, after the default constraint of the column, which would block the drop.
   *
   * @param operation the operation
   */
  record DropColumn(Operation.DropColumn operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      Table after = sql.tableAfter(operation.table());
      Table before = sql.before().tableById(after.id()).orElseThrow();
      if (before.column(operation.column()).map(Column::defaultValue).isPresent()) {
        dropDefault(sql, operation.table(), operation.column());
      }
      operation.render(sql);
    }
  }

  /**
   * {@code ALTER TABLE ... ALTER COLUMN}. SQL Server cannot change a column while an index, a key, a check or a
   * default uses it, so these are dropped before the change and created again after it. SQL Server cannot add or
   * remove {@code IDENTITY} on an existing column.
   *
   * @param operation the operation
   */
  record AlterColumn(Operation.AlterColumn operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      String table = operation.table();
      Column from = operation.from();
      Column to = operation.to();
      if (operation.identityChanged()) {
        throw new SchemaException("SQL Server cannot " + (to.identity() ? "add IDENTITY to" : "remove IDENTITY from")
            + " the existing column " + table + "." + to.name() + ". Add a new column or a new table instead.");
      }
      boolean alter = operation.typeChanged() || operation.becomesNotNull() || operation.becomesNullable()
          || operation.collationChanged(sql.dialect().name());
      if (alter) {
        change(sql);
      } else if (operation.defaultChanged()) {
        if (from.defaultValue() != null) {
          dropDefault(sql, table, to.name());
        }
        if (to.defaultValue() != null) {
          addDefault(sql, table, to);
        }
      }
      if (operation.commentChanged()) {
        sql.render(new ColumnComment(table, to.name(), from.comment(), to.comment())).end();
      }
    }

    private void change(DdlWriter sql) {
      String table = operation.table();
      Column to = operation.to();
      String column = to.name();
      Table after = sql.tableAfter(table);
      Table before = sql.before().tableById(after.id()).orElseThrow();
      List<Index> indexes = after.indexes().stream().filter(index -> index.columns().contains(column))
          .filter(index -> before.indexes().stream().anyMatch(was -> was.name().equals(index.name()))).toList();
      List<Operation.AddForeignKey> keys = new ArrayList<>();
      for (Table other : sql.after().tables()) {
        Table otherBefore = sql.before().tableById(other.id()).orElse(null);
        for (ForeignKey key : other.foreignKeys()) {
          boolean uses = other.name().equals(table) && key.columns().contains(column)
              || key.table().equals(table) && key.referencedColumns().contains(column);
          boolean kept = otherBefore != null
              && otherBefore.foreignKeys().stream().anyMatch(was -> was.name().equals(key.name()));
          if (uses && kept) {
            keys.add(new Operation.AddForeignKey(other.name(), key));
          }
        }
      }
      List<Check> checks = after.checks().stream().filter(before.checks()::contains).toList();
      boolean primaryKey = after.primaryKey().columns().contains(column)
          && before.primaryKey().name().equals(after.primaryKey().name())
          && before.primaryKey().columns().size() == after.primaryKey().columns().size();
      for (Operation.AddForeignKey key : keys) {
        sql.append("ALTER TABLE ").table(key.table()).append(" DROP CONSTRAINT ").name(key.key().name()).end();
      }
      for (Index index : indexes) {
        sql.append("DROP INDEX ").name(index.name()).append(" ON ").table(table).end();
      }
      for (Check check : checks) {
        sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(check.name()).end();
      }
      if (primaryKey) {
        sql.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(after.primaryKey().name()).end();
      }
      if (operation.from().defaultValue() != null) {
        dropDefault(sql, table, column);
      }
      if (operation.becomesNotNull()) {
        sql.append("UPDATE ").table(table).append(" SET ").name(column).append(" = " + sql.dialect().fill(to)
            + " WHERE ").name(column).append(" IS NULL").end();
      }
      sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ");
      Definition.type(sql, after, to.withIdentity(false));
      sql.end();
      if (primaryKey) {
        sql.append("ALTER TABLE ").table(table).append(" ADD CONSTRAINT ").name(after.primaryKey().name())
            .append(" PRIMARY KEY (").names(after.primaryKey().columns()).append(")").end();
      }
      if (to.defaultValue() != null) {
        addDefault(sql, table, to);
      }
      for (Check check : checks) {
        sql.render(new Operation.AddCheck(table, check)).end();
      }
      for (Index index : indexes) {
        sql.render(new Operation.AddIndex(table, index)).end();
      }
      for (Operation.AddForeignKey key : keys) {
        sql.render(key).end();
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
      sql.append("DROP INDEX ").name(operation.index().name()).append(" ON ").oldTable(operation.table());
    }
  }

  /**
   * Adds a foreign key. SQL Server has no {@code RESTRICT}. A restricting foreign key uses the default,
   * {@code NO ACTION}, which also rejects the change.
   *
   * @param operation the operation
   */
  record AddForeignKey(Operation.AddForeignKey operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      ForeignKey key = operation.key();
      ForeignKey supported = new ForeignKey(key.name(), key.columns(), key.table(), key.referencedColumns(),
          supported(key.onDelete()), supported(key.onUpdate()));
      sql.append("ALTER TABLE ").table(operation.table()).append(" ADD ");
      Operation.AddForeignKey.constraint(sql, supported);
    }

    private static ForeignKey.Action supported(ForeignKey.Action action) {
      return action == ForeignKey.Action.RESTRICT ? ForeignKey.Action.NO_ACTION : action;
    }
  }

  /**
   * Sets the comment of a table as its {@code MS_Description} extended property.
   *
   * @param operation the operation
   */
  record CommentTable(Operation.CommentTable operation) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      describe(sql, operation.table(), null, operation.from(), operation.to());
    }
  }

  /**
   * Sets the comment of a column as its {@code MS_Description} extended property.
   *
   * @param comment the comment node
   */
  record CommentColumn(ColumnComment comment) implements SqlServerOperation {

    @Override
    public void render(DdlWriter sql) {
      describe(sql, comment.table(), comment.column(), comment.from(), comment.to());
    }
  }

  /**
   * Returns a variable name for a statement. Flyway sends statements that declare variables in one batch, where each
   * variable name must be unique, so the name depends on what the statement changes.
   */
  private static String variable(String purpose, String... parts) {
    return "@leadseal_" + purpose + "_" + Integer.toHexString(String.join(".", parts).hashCode());
  }

  /** Returns the quoted, schema-qualified name of an object as a string literal. */
  private static String name(DdlWriter sql, String schema, String name) {
    return sql.dialect().string(qualified(sql, schema, name));
  }

  /** Returns the quoted, schema-qualified name of an object. */
  private static String qualified(DdlWriter sql, String schema, String name) {
    return (schema == null ? "" : sql.dialect().quote(schema) + ".") + sql.dialect().quote(name);
  }

  /**
   * Drops the default constraint of a column, whatever its name. Renames do not rename default constraints, so the
   * migration looks the name up.
   */
  private static void dropDefault(DdlWriter sql, String table, String column) {
    String object = name(sql, sql.tableAfter(table).schema(), table);
    String drop = sql.dialect().string("ALTER TABLE " + qualified(sql, sql.tableAfter(table).schema(), table)
        + " DROP CONSTRAINT ");
    String statement = variable("default", table, column);
    sql.append("DECLARE " + statement + " nvarchar(max) = (SELECT " + drop + " + QUOTENAME(d.name) FROM"
        + " sys.default_constraints d JOIN sys.columns c ON c.object_id = d.parent_object_id AND c.column_id ="
        + " d.parent_column_id WHERE d.parent_object_id = OBJECT_ID(" + object + ") AND c.name = "
        + sql.dialect().string(column) + ") IF " + statement + " IS NOT NULL EXEC(" + statement + ")").end();
  }

  /** Adds the default constraint of a column. */
  private static void addDefault(DdlWriter sql, String table, Column column) {
    sql.append("ALTER TABLE ").table(table).append(" ADD CONSTRAINT ")
        .name(Default.constraintName(table, column.name()))
        .append(" DEFAULT " + sql.dialect().defaultValue(column) + " FOR ").name(column.name()).end();
  }

  /** Replaces the {@code MS_Description} extended property of a table, or of a column if it is not {@code null}. */
  private static void describe(DdlWriter sql, String table, String column, String from, String to) {
    String schema = sql.tableAfter(table).schema();
    String value = schema == null ? "SCHEMA_NAME()" : sql.dialect().string(schema);
    String target = ", @level1type = N'TABLE', @level1name = " + sql.dialect().string(table)
        + (column == null ? "" : ", @level2type = N'COLUMN', @level2name = " + sql.dialect().string(column));
    String key = column == null ? table : table + "." + column;
    if (from != null) {
      String variable = variable("drop_comment", key);
      sql.append("DECLARE " + variable + " sysname = " + value + " EXEC sp_dropextendedproperty @name ="
          + " N'MS_Description', @level0type = N'SCHEMA', @level0name = " + variable + target).end();
    }
    if (to != null) {
      String variable = variable("add_comment", key);
      sql.append("DECLARE " + variable + " sysname = " + value + " EXEC sp_addextendedproperty @name ="
          + " N'MS_Description', @value = " + sql.dialect().string(to) + ", @level0type = N'SCHEMA', @level0name = "
          + variable + target).end();
    }
  }
}
