package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.change.AddColumnAttributeChange;
import dev.horizon.leadseal.change.AddColumnChange;
import dev.horizon.leadseal.change.AddTableAttributeChange;
import dev.horizon.leadseal.change.AlterColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnTypeChange;
import dev.horizon.leadseal.change.AlterTableAttributeChange;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateTableChange;
import dev.horizon.leadseal.change.DropColumnAttributeChange;
import dev.horizon.leadseal.change.DropColumnChange;
import dev.horizon.leadseal.change.DropTableAttributeChange;
import dev.horizon.leadseal.change.DropTableChange;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommandFactory;
import dev.horizon.leadseal.command.EmptyCommand;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The commands of SQLite. A table with a change that SQLite cannot make in place is rebuilt once, after the other
 * commands: a changed column type, nullability, default or collation, a changed primary key, an added or dropped
 * foreign key or check, a new column with a generated default, or a dropped column that is part of the primary key or
 * a foreign key.
 *
 * <p>
 * When a foreign key references a rebuilt table, the migration turns foreign keys off while it runs. SQLite allows
 * this only outside a transaction. It fails before it changes a table if foreign keys are still on, and after the
 * rebuild if a row breaks a foreign key.
 */
class SqliteCommandFactory extends CommandFactory {

  @Override
  public List<Command> plan(List<Change> changes, ScriptWriter sql) {
    Stream.concat(sql.before().names().stream(), sql.after().names().stream())
        .filter(name -> name.namespace() != null).findFirst().ifPresent(name -> {
          throw new SchemaException("SQLite has no namespaces, so the table '" + name.name() + "' cannot be in the"
              + " namespace '" + name.namespace() + "'.");
        });
    Set<TableName> created = new LinkedHashSet<>();
    Set<TableName> dropped = new LinkedHashSet<>();
    for (Change change : changes) {
      if (change instanceof CreateTableChange) {
        created.add(change.table());
      } else if (change instanceof DropTableChange) {
        dropped.add(change.table());
      }
    }
    Set<String> rebuilt = rebuilt(changes, sql, created, dropped);
    boolean referenced = sql.before().names().stream().map(name -> sql.tableBefore(name))
        .flatMap(table -> table.attributes(ForeignKeyAttribute.class).stream())
        .anyMatch(key -> sql.before().table(key.table()).map(Table::id).filter(rebuilt::contains).isPresent());
    List<Command> commands = new ArrayList<>();
    if (referenced) {
      commands.add(new SqliteForeignKeysOffCommand());
    } else if (!rebuilt.isEmpty()) {
      commands.add(new SqliteDeferForeignKeysCommand());
    }
    for (Change change : changes) {
      boolean inlined = change instanceof AddTableAttributeChange add && created.contains(add.table());
      boolean gone = change instanceof DropTableAttributeChange drop && dropped.contains(drop.table());
      boolean rebuilding = tableId(change, sql).filter(rebuilt::contains).isPresent();
      if (!inlined && !gone && !rebuilding) {
        commands.addAll(commands(change, sql));
      }
    }
    for (TableName name : sql.after().names()) {
      Table table = sql.tableAfter(name);
      if (rebuilt.contains(table.id())) {
        commands.add(new SqliteRebuildCommand(sql.before().nameOf(table.id()).orElseThrow(), name));
      }
    }
    if (referenced) {
      commands.add(new SqliteForeignKeysOnCommand());
    }
    return commands;
  }

  /** Returns the ids of the tables that need a rebuild. */
  private static Set<String> rebuilt(List<Change> changes, ScriptWriter sql, Set<TableName> created,
      Set<TableName> dropped) {
    Set<String> ids = new LinkedHashSet<>();
    for (Change change : changes) {
      boolean rebuild = switch (change) {
        case AlterColumnTypeChange alter -> true;
        case AddColumnAttributeChange add -> rewritten(add.attribute(), sql);
        case DropColumnAttributeChange drop -> rewritten(drop.attribute(), sql);
        case AlterColumnAttributeChange alter -> rewritten(alter.to(), sql);
        case AlterTableAttributeChange alter -> alter.to() instanceof PrimaryKeyAttribute;
        case AddTableAttributeChange add -> !created.contains(add.table())
            && (add.attribute() instanceof ForeignKeyAttribute || add.attribute() instanceof CheckAttribute);
        case DropTableAttributeChange drop -> !dropped.contains(drop.table())
            && (drop.attribute() instanceof ForeignKeyAttribute || drop.attribute() instanceof CheckAttribute);
        case AddColumnChange add -> add.column().attribute(DefaultAttribute.class)
            .filter(value -> !(value instanceof LiteralDefaultAttribute)).isPresent();
        case DropColumnChange drop -> keyed(drop, sql);
        default -> false;
      };
      if (rebuild) {
        tableId(change, sql).ifPresent(ids::add);
      }
    }
    return ids;
  }

  /** Returns whether a column attribute is part of the SQLite column definition. */
  private static boolean rewritten(ColumnAttribute attribute, ScriptWriter sql) {
    return attribute instanceof NotNullAttribute || attribute instanceof DefaultAttribute
        || attribute instanceof CollationAttribute collation && collation.dialect().equals(sql.dialect().name());
  }

  private static boolean keyed(DropColumnChange drop, ScriptWriter sql) {
    Table before = sql.previous(drop.table()).orElseThrow();
    return before.primaryKey().columns().contains(drop.column())
        || before.attributes(ForeignKeyAttribute.class).stream().anyMatch(key -> key.columns().contains(drop.column()));
  }

  /** Returns the id of the table of a change, in the schema before or after the migration. */
  private static Optional<String> tableId(Change change, ScriptWriter sql) {
    boolean old = change instanceof DropTableAttributeChange || change instanceof DropTableChange;
    Optional<Table> first = old ? sql.before().table(change.table()) : sql.after().table(change.table());
    Optional<Table> second = old ? sql.after().table(change.table()) : sql.before().table(change.table());
    return first.or(() -> second).map(Table::id);
  }

  @Override
  public Command createSchema(String schema) {
    throw new SchemaException("SQLite has no namespaces, so it cannot create the namespace '" + schema + "'.");
  }

  @Override
  public Command createTable(TableName table, Table definition) {
    return new SqliteCreateTableCommand(table, definition);
  }

  @Override
  public Command columnDefinition(Table definition, Column column) {
    return new SqliteColumnDefinitionCommand(definition, column);
  }

  @Override
  public Command renameTable(TableName from, String to) {
    return new SqliteRenameTableCommand(from, to);
  }

  @Override
  public Command addColumn(TableName table, Column column) {
    return new SqliteAddColumnCommand(table, column);
  }

  @Override
  public Command addIdentity(TableName table, String column) {
    return new EmptyCommand();
  }

  @Override
  public Command dropIdentity(TableName table, String column) {
    return new EmptyCommand();
  }

  @Override
  public Command setColumnComment(TableName table, String column, CommentAttribute from, CommentAttribute to) {
    return new EmptyCommand();
  }

  @Override
  public Command setTableComment(TableName table, CommentAttribute from, CommentAttribute to) {
    return new EmptyCommand();
  }
}
