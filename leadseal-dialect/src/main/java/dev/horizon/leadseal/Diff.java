package dev.horizon.leadseal;

import dev.horizon.leadseal.Operation.AddCheck;
import dev.horizon.leadseal.Operation.AddColumn;
import dev.horizon.leadseal.Operation.AddForeignKey;
import dev.horizon.leadseal.Operation.AddIndex;
import dev.horizon.leadseal.Operation.AlterColumn;
import dev.horizon.leadseal.Operation.AlterPrimaryKey;
import dev.horizon.leadseal.Operation.CommentTable;
import dev.horizon.leadseal.Operation.CreateTable;
import dev.horizon.leadseal.Operation.DropCheck;
import dev.horizon.leadseal.Operation.DropColumn;
import dev.horizon.leadseal.Operation.DropForeignKey;
import dev.horizon.leadseal.Operation.DropIndex;
import dev.horizon.leadseal.Operation.DropTable;
import dev.horizon.leadseal.Operation.MoveTable;
import dev.horizon.leadseal.Operation.RenameColumn;
import dev.horizon.leadseal.Operation.RenameTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The operations that change one schema into another.
 *
 * <p>
 * Tables and columns are matched by id. A table or column that keeps its id but changes its name is renamed, and keeps
 * its data. Indexes, checks and foreign keys are matched by name and definition. A changed index, check or foreign key
 * is dropped and added again.
 *
 * <p>
 * The operations run in this order: drop foreign keys, drop checks, drop indexes, drop tables, rename tables, move
 * tables to other schemas, create tables, rename columns, add columns, alter columns, alter primary keys, drop columns,
 * add indexes, add checks, add foreign keys, change table comments.
 */
public final class Diff {

  private final Schema before;
  private final Schema after;
  private final List<Operation> dropKeys = new ArrayList<>();
  private final List<Operation> dropChecks = new ArrayList<>();
  private final List<Operation> dropIndexes = new ArrayList<>();
  private final List<Operation> dropTables = new ArrayList<>();
  private final List<Operation> renameTables = new ArrayList<>();
  private final List<Operation> moveTables = new ArrayList<>();
  private final List<Operation> createTables = new ArrayList<>();
  private final List<Operation> renameColumns = new ArrayList<>();
  private final List<Operation> addColumns = new ArrayList<>();
  private final List<Operation> alterColumns = new ArrayList<>();
  private final List<Operation> alterKeys = new ArrayList<>();
  private final List<Operation> dropColumns = new ArrayList<>();
  private final List<Operation> addIndexes = new ArrayList<>();
  private final List<Operation> addChecks = new ArrayList<>();
  private final List<Operation> addKeys = new ArrayList<>();
  private final List<Operation> commentTables = new ArrayList<>();

  public Diff(Schema before, Schema after) {
    this.before = before;
    this.after = after;
    for (Table old : before.tables()) {
      Optional<Table> current = after.tableById(old.id());
      if (current.isEmpty()) {
        old.foreignKeys().forEach(key -> dropKeys.add(new DropForeignKey(old.name(), key)));
        dropTables.add(new DropTable(old.name()));
      } else {
        table(old, current.get());
      }
    }
    for (Table table : after.tables()) {
      if (before.tableById(table.id()).isEmpty()) {
        createTables.add(new CreateTable(table));
        table.foreignKeys().forEach(key -> addKeys.add(new AddForeignKey(table.name(), key)));
      }
    }
  }

  /**
   * Returns the operations, in the order they run.
   *
   * @return the operations, or an empty list if the schemas have the same tables
   */
  public List<Operation> operations() {
    List<Operation> all = new ArrayList<>();
    for (List<Operation> group : List.of(dropKeys, dropChecks, dropIndexes, dropTables, renameTables, moveTables,
        createTables, renameColumns, addColumns, alterColumns, alterKeys, dropColumns, addIndexes, addChecks, addKeys,
        commentTables)) {
      all.addAll(group);
    }
    return all;
  }

  private void table(Table old, Table current) {
    if (!old.name().equals(current.name())) {
      renameTables.add(new RenameTable(old.name(), current.name()));
    }
    if (!Objects.equals(old.schema(), current.schema())) {
      moveTables.add(new MoveTable(current.name(), old.schema(), current.schema()));
    }
    if (!Objects.equals(old.comment(), current.comment())) {
      commentTables.add(new CommentTable(current.name(), old.comment(), current.comment()));
    }
    for (Check check : old.checks()) {
      if (!current.checks().contains(check)) {
        dropChecks.add(new DropCheck(old.name(), check));
      }
    }
    for (Check check : current.checks()) {
      if (!old.checks().contains(check)) {
        addChecks.add(new AddCheck(current.name(), check));
      }
    }
    for (Column column : old.columns()) {
      Optional<Column> now = current.columnById(column.id());
      if (now.isEmpty()) {
        dropColumns.add(new DropColumn(current.name(), column.name()));
        continue;
      }
      Column renamed = column.withName(now.get().name());
      if (!column.name().equals(renamed.name())) {
        renameColumns.add(new RenameColumn(current.name(), column.name(), renamed.name()));
      }
      if (!renamed.equals(now.get())) {
        alterColumns.add(new AlterColumn(current.name(), renamed, now.get()));
      }
    }
    for (Column column : current.columns()) {
      if (old.columnById(column.id()).isEmpty()) {
        addColumns.add(new AddColumn(current.name(), column));
      }
    }
    List<String> oldKey = renamed(old, current, old.primaryKey().columns());
    if (!oldKey.equals(current.primaryKey().columns())) {
      String name = old.name().equals(current.name()) ? old.primaryKey().name() : current.primaryKey().name();
      alterKeys.add(new AlterPrimaryKey(current.name(), new PrimaryKey(name, oldKey), current.primaryKey()));
    }
    for (Index index : old.indexes()) {
      Index mapped = index.withColumns(renamed(old, current, index.columns()));
      if (!current.indexes().contains(mapped)) {
        dropIndexes.add(new DropIndex(old.name(), index));
      }
    }
    for (Index index : current.indexes()) {
      boolean kept = old.indexes().stream()
          .anyMatch(was -> was.withColumns(renamed(old, current, was.columns())).equals(index));
      if (!kept) {
        addIndexes.add(new AddIndex(current.name(), index));
      }
    }
    for (ForeignKey key : old.foreignKeys()) {
      if (!current.foreignKeys().contains(mapped(old, current, key))) {
        dropKeys.add(new DropForeignKey(old.name(), key));
      }
    }
    for (ForeignKey key : current.foreignKeys()) {
      boolean kept = old.foreignKeys().stream().anyMatch(was -> mapped(old, current, was).equals(key));
      if (!kept) {
        addKeys.add(new AddForeignKey(current.name(), key));
      }
    }
  }

  /** Returns a foreign key of the old table with its columns, referenced table and referenced columns renamed. */
  private ForeignKey mapped(Table old, Table current, ForeignKey key) {
    List<String> columns = renamed(old, current, key.columns());
    Optional<Table> oldTarget = before.table(key.table());
    Optional<Table> newTarget = oldTarget.flatMap(target -> after.tableById(target.id()));
    if (oldTarget.isEmpty() || newTarget.isEmpty()) {
      return key.withColumns(columns, key.table(), key.referencedColumns());
    }
    return key.withColumns(columns, newTarget.get().name(),
        renamed(oldTarget.get(), newTarget.get(), key.referencedColumns()));
  }

  /** Returns the names that columns of the old table have in the new table, or their old names if they are gone. */
  private static List<String> renamed(Table old, Table current, List<String> columns) {
    List<String> names = new ArrayList<>();
    for (String column : columns) {
      String id = old.column(column).map(Column::id).orElse(null);
      names.add(current.columnById(Objects.requireNonNullElse(id, "")).map(Column::name).orElse(column));
    }
    return names;
  }
}
