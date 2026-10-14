package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.change.AddColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnTypeChange;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.DropColumnAttributeChange;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommandFactory;
import dev.horizon.leadseal.command.EmptyCommand;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The commands of MariaDB. MariaDB changes a column with {@code MODIFY COLUMN}, which writes its whole definition, so
 * all changes of one column become one {@link MariaDbModifyColumnCommand}.
 */
class MariaDbCommandFactory extends CommandFactory {

  @Override
  public List<Command> plan(List<Change> changes, ScriptWriter sql) {
    List<Command> commands = new ArrayList<>();
    Set<List<Object>> modified = new HashSet<>();
    for (Change change : changes) {
      Optional<String> column = modifiedColumn(change, sql);
      if (column.isEmpty()) {
        commands.addAll(commands(change, sql));
      } else if (modified.add(List.of(change.table(), column.get()))) {
        commands.add(new MariaDbModifyColumnCommand(change.table(), column.get()));
      }
    }
    return commands;
  }

  /** Returns the column that a change modifies, if MariaDB writes the change as a new column definition. */
  private static Optional<String> modifiedColumn(Change change, ScriptWriter sql) {
    return switch (change) {
      case AlterColumnTypeChange alter -> Optional.of(alter.column());
      case AddColumnAttributeChange add -> written(add.attribute(), sql) ? Optional.of(add.column()) : Optional.empty();
      case DropColumnAttributeChange drop -> written(drop.attribute(), sql) ? Optional.of(drop.column())
          : Optional.empty();
      case AlterColumnAttributeChange alter -> written(alter.to(), sql) ? Optional.of(alter.column())
          : Optional.empty();
      default -> Optional.empty();
    };
  }

  /** Returns whether an attribute is part of the MariaDB column definition: all but the collations of others. */
  private static boolean written(ColumnAttribute attribute, ScriptWriter sql) {
    return !(attribute instanceof CollationAttribute collation) || collation.dialect().equals(sql.dialect().name());
  }

  @Override
  public Command columnDefinition(Table definition, Column column) {
    return new MariaDbColumnDefinitionCommand(definition, column);
  }

  @Override
  public Command addColumn(TableName table, Column column) {
    return new MariaDbAddColumnCommand(table, column);
  }

  @Override
  public Command renameTable(TableName from, String to) {
    return new MariaDbRenameTableCommand(from, to);
  }

  @Override
  public Command moveTable(TableName from, String to) {
    return new MariaDbMoveTableCommand(from, to);
  }

  @Override
  public Command alterPrimaryKey(TableName table, PrimaryKeyAttribute from, PrimaryKeyAttribute to) {
    return new MariaDbAlterPrimaryKeyCommand(table, from, to);
  }

  @Override
  public Command addIndex(TableName table, IndexAttribute index) {
    return new MariaDbAddIndexCommand(table, index);
  }

  @Override
  public Command dropIndex(TableName table, IndexAttribute index) {
    return new MariaDbDropIndexCommand(table, index);
  }

  @Override
  public Command dropForeignKey(TableName table, ForeignKeyAttribute key) {
    return new MariaDbDropForeignKeyCommand(table, key);
  }

  @Override
  public Command setTableComment(TableName table, CommentAttribute from, CommentAttribute to) {
    return new MariaDbSetTableCommentCommand(table, from, to);
  }

  @Override
  public Command setColumnComment(TableName table, String column, CommentAttribute from, CommentAttribute to) {
    return new EmptyCommand();
  }
}
