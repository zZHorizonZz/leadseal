package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Change;
import dev.horizon.leadseal.Check;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Node;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A change as SQLite runs it.
 *
 * <p>
 * Foreign keys and checks of new tables go into their {@code CREATE TABLE}. A table with a change that SQLite cannot
 * make in place is rebuilt once, after the other operations: a changed column type, nullability, default or collation,
 * a changed primary key, an added or dropped foreign key or check, a new column with a generated default, or a dropped
 * column that is part of the primary key or a foreign key. SQLite has no comments, so comment changes write nothing.
 *
 * <p>
 * A rebuild drops the old table. When a foreign key references that table, the drop would run the foreign key's
 * {@code ON DELETE} action on the referencing rows. Such a migration turns foreign keys off while it runs, which
 * SQLite allows only outside a transaction. It fails before it changes a table if foreign keys are still on, and
 * after the rebuild if a row breaks a foreign key.
 *
 * @param change the change
 */
record SqliteChange(Change change) implements Node {

  private static final Node NOTHING = sql -> {
  };

  @Override
  public void render(DdlWriter sql) {
    Stream.concat(sql.before().tables().stream(), sql.after().tables().stream())
        .filter(table -> table.schema() != null).findFirst().ifPresent(table -> {
          throw new SchemaException("SQLite has no database schemas, so the table '" + table.name() + "' cannot be"
              + " in the schema '" + table.schema() + "'.");
        });
    List<Operation> operations = change.operations();
    Set<String> created = new LinkedHashSet<>();
    Set<String> dropped = new LinkedHashSet<>();
    for (Operation operation : operations) {
      if (operation instanceof Operation.CreateTable create) {
        created.add(create.table());
      } else if (operation instanceof Operation.DropTable drop) {
        dropped.add(drop.table());
      }
    }
    Set<String> rebuilt = rebuilt(sql, operations, created, dropped);
    List<Node> nodes = new ArrayList<>();
    for (Operation operation : operations) {
      if (operation instanceof Operation.AddForeignKey add && created.contains(add.table())) {
        continue;
      }
      if ((operation instanceof Operation.DropForeignKey || operation instanceof Operation.DropCheck)
          && dropped.contains(operation.table())) {
        continue;
      }
      if (tableId(sql, operation).filter(rebuilt::contains).isPresent()) {
        continue;
      }
      nodes.add(switch (operation) {
        case Operation.CreateTable create -> new SqliteOperation.CreateTable(create.definition(),
            sql.tableAfter(create.table()).foreignKeys());
        case Operation.RenameTable rename -> new SqliteOperation.RenameTable(rename);
        case Operation.AddColumn add -> new SqliteOperation.AddColumn(add);
        case Operation.AlterColumn alter -> NOTHING;
        case Operation.CommentTable comment -> NOTHING;
        default -> operation;
      });
    }
    boolean referenced = sql.before().tables().stream().flatMap(table -> table.foreignKeys().stream())
        .anyMatch(key -> sql.before().table(key.table()).map(Table::id).filter(rebuilt::contains).isPresent());
    if (referenced) {
      sql.outsideTransaction();
      sql.append("PRAGMA foreign_keys = OFF").end();
      sql.append("CREATE TEMP TABLE leadseal_check (foreign_keys INTEGER CONSTRAINT foreign_keys_must_be_off"
          + " CHECK (foreign_keys = 0), violations INTEGER CONSTRAINT foreign_keys_must_hold CHECK (violations = 0))")
          .end();
      sql.append("INSERT INTO leadseal_check (foreign_keys) SELECT foreign_keys FROM pragma_foreign_keys").end();
    } else if (!rebuilt.isEmpty()) {
      sql.append("PRAGMA defer_foreign_keys = ON").end();
    }
    for (Table table : sql.after().tables()) {
      if (rebuilt.contains(table.id())) {
        nodes.add(new SqliteOperation.Rebuild(sql.before().tableById(table.id()).orElseThrow(), table));
      }
    }
    for (Node node : nodes) {
      sql.render(node).end();
    }
    if (referenced) {
      sql.append("INSERT INTO leadseal_check (violations) SELECT COUNT(*) FROM pragma_foreign_key_check").end();
      sql.append("DROP TABLE leadseal_check").end();
      sql.append("PRAGMA foreign_keys = ON").end();
    }
  }

  /** Returns the ids of the tables that need a rebuild. */
  private static Set<String> rebuilt(DdlWriter sql, List<Operation> operations, Set<String> created,
      Set<String> dropped) {
    Set<String> ids = new LinkedHashSet<>();
    for (Operation operation : operations) {
      switch (operation) {
        case Operation.AlterColumn alter when alter.typeChanged() || alter.becomesNotNull() || alter.becomesNullable()
            || alter.defaultChanged() || alter.collationChanged(sql.dialect().name()) ->
          ids.add(sql.tableAfter(alter.table()).id());
        case Operation.AlterPrimaryKey alter -> ids.add(sql.tableAfter(alter.table()).id());
        case Operation.AddForeignKey add when !created.contains(add.table()) ->
          ids.add(sql.tableAfter(add.table()).id());
        case Operation.DropForeignKey drop when !dropped.contains(drop.table()) ->
          ids.add(sql.tableBefore(drop.table()).id());
        case Operation.AddCheck add -> ids.add(sql.tableAfter(add.table()).id());
        case Operation.DropCheck drop when !dropped.contains(drop.table()) ->
          ids.add(sql.tableBefore(drop.table()).id());
        case Operation.AddColumn add when add.column().defaultValue() != null
            && add.column().defaultValue().kind() != Default.Kind.VALUE ->
          ids.add(sql.tableAfter(add.table()).id());
        case Operation.DropColumn drop -> {
          Table after = sql.tableAfter(drop.table());
          Table before = sql.before().tableById(after.id()).orElseThrow();
          boolean keyed = before.primaryKey().columns().contains(drop.column())
              || before.foreignKeys().stream().anyMatch(key -> key.columns().contains(drop.column()));
          if (keyed) {
            ids.add(after.id());
          }
        }
        default -> {
        }
      }
    }
    return ids;
  }

  /** Returns the id of the table that an operation changes, if that table exists before or after the change. */
  private static Optional<String> tableId(DdlWriter sql, Operation operation) {
    boolean beforeRenames = operation instanceof Operation.DropIndex || operation instanceof Operation.DropForeignKey
        || operation instanceof Operation.DropCheck || operation instanceof Operation.DropTable;
    return (beforeRenames ? sql.before().table(operation.table()) : sql.after().table(operation.table()))
        .map(Table::id);
  }

  /** The SQLite forms of the operations that SQLite writes differently from standard SQL. */
  sealed interface SqliteOperation extends Node {

    /**
     * {@code CREATE TABLE} with its foreign keys and checks inline, and its indexes.
     *
     * @param table the table
     * @param keys the foreign keys
     */
    record CreateTable(Table table, List<ForeignKey> keys) implements SqliteOperation {

      @Override
      public void render(DdlWriter sql) {
        create(sql, table, table.name(), keys);
        for (var index : table.indexes()) {
          sql.render(new Operation.AddIndex(table.name(), index)).end();
        }
      }
    }

    /**
     * {@code ALTER TABLE ... RENAME TO}. SQLite cannot rename a constraint, so the primary key keeps its name.
     *
     * @param operation the operation
     */
    record RenameTable(Operation.RenameTable operation) implements SqliteOperation {

      @Override
      public void render(DdlWriter sql) {
        sql.append("ALTER TABLE ").name(operation.from()).append(" RENAME TO ").name(operation.table());
      }
    }

    /**
     * {@code ALTER TABLE ... ADD COLUMN}. A {@code NOT NULL} column without a default keeps its zero value as default,
     * because SQLite cannot drop a default.
     *
     * @param operation the operation
     */
    record AddColumn(Operation.AddColumn operation) implements SqliteOperation {

      @Override
      public void render(DdlWriter sql) {
        Column column = operation.column();
        sql.append("ALTER TABLE ").name(operation.table()).append(" ADD COLUMN ")
            .column(sql.tableAfter(operation.table()), column);
        if (!column.nullable() && column.defaultValue() == null) {
          sql.append(" DEFAULT " + sql.dialect().zero(column));
        }
      }
    }

    /**
     * Rebuilds a table: creates the new table under a temporary name, copies the rows, drops the old table, renames the
     * new one and creates its indexes. Columns are matched by id. A new column gets its default, or {@code NULL} if it
     * accepts {@code NULL}, or its zero value.
     *
     * @param before the table before the change
     * @param after the table after the change
     */
    record Rebuild(Table before, Table after) implements SqliteOperation {

      @Override
      public void render(DdlWriter sql) {
        String temporary = "leadseal_new_" + after.name();
        create(sql, after, temporary, after.foreignKeys());
        List<Column> copied = after.columns().stream()
            .filter(column -> before.columnById(column.id()).isPresent() || column.defaultValue() == null).toList();
        sql.append("INSERT INTO ").name(temporary).append(" (").names(copied.stream().map(Column::name).toList())
            .append(") SELECT ");
        for (int i = 0; i < copied.size(); i++) {
          Column column = copied.get(i);
          sql.append(i == 0 ? "" : ", ");
          Optional<Column> old = before.columnById(column.id());
          if (old.isEmpty()) {
            sql.append(column.nullable() ? "NULL" : sql.dialect().zero(column));
          } else if (!column.nullable() && old.get().nullable()) {
            sql.append("COALESCE(").name(old.get().name()).append(", " + sql.dialect().fill(column) + ")");
          } else {
            sql.name(old.get().name());
          }
        }
        sql.append(" FROM ").name(before.name()).end();
        sql.append("DROP TABLE ").name(before.name()).end();
        sql.append("ALTER TABLE ").name(temporary).append(" RENAME TO ").name(after.name()).end();
        for (var index : after.indexes()) {
          sql.render(new Operation.AddIndex(after.name(), index)).end();
        }
      }
    }

    private static void create(DdlWriter sql, Table table, String name, List<ForeignKey> keys) {
      sql.append("CREATE TABLE ").name(name).append(" (");
      for (Column column : table.columns()) {
        sql.column(table, column).append(", ");
      }
      sql.append("CONSTRAINT ").name(table.primaryKey().name()).append(" PRIMARY KEY (")
          .names(table.primaryKey().columns()).append(")");
      for (Check check : table.checks()) {
        sql.append(", ");
        Operation.AddCheck.constraint(sql, check);
      }
      for (ForeignKey key : keys) {
        sql.append(", ");
        Operation.AddForeignKey.constraint(sql, key);
      }
      sql.append(")").end();
    }
  }
}
