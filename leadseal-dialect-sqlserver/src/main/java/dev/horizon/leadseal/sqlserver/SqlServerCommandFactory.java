package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.change.AddColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnTypeChange;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.DropColumnAttributeChange;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommandFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The commands of SQL Server. The type, collation and nullability changes of one column become one
 * {@link SqlServerAlterColumnCommand}, which also sets the default of the column again.
 */
class SqlServerCommandFactory extends CommandFactory {

  @Override
  public List<Command> plan(List<Change> changes, ScriptWriter sql) {
    Set<List<Object>> altered = new HashSet<>();
    for (Change change : changes) {
      if (altering(change, sql)) {
        altered.add(List.of(change.table(), column(change).orElseThrow()));
      }
    }
    List<Command> commands = new ArrayList<>();
    Set<List<Object>> written = new HashSet<>();
    for (Change change : changes) {
      Optional<List<Object>> key = column(change).map(column -> List.of(change.table(), column));
      if (key.isEmpty() || !altered.contains(key.get())) {
        commands.addAll(commands(change, sql));
      } else if (altering(change, sql) || defaulting(change)) {
        if (written.add(key.get())) {
          commands.add(new SqlServerAlterColumnCommand(change.table(), (String) key.get().get(1)));
        }
      } else {
        commands.addAll(commands(change, sql));
      }
    }
    return commands;
  }

  private static Optional<String> column(Change change) {
    return switch (change) {
      case AlterColumnTypeChange alter -> Optional.of(alter.column());
      case AddColumnAttributeChange add -> Optional.of(add.column());
      case DropColumnAttributeChange drop -> Optional.of(drop.column());
      case AlterColumnAttributeChange alter -> Optional.of(alter.column());
      default -> Optional.empty();
    };
  }

  /** Returns whether a change needs {@code ALTER COLUMN}: a new type, nullability or collation of SQL Server. */
  private static boolean altering(Change change, ScriptWriter sql) {
    return switch (change) {
      case AlterColumnTypeChange alter -> true;
      case AddColumnAttributeChange add -> altering(add.attribute(), sql);
      case DropColumnAttributeChange drop -> altering(drop.attribute(), sql);
      case AlterColumnAttributeChange alter -> altering(alter.to(), sql);
      default -> false;
    };
  }

  private static boolean altering(ColumnAttribute attribute, ScriptWriter sql) {
    return attribute instanceof NotNullAttribute
        || attribute instanceof CollationAttribute collation && collation.dialect().equals(sql.dialect().name());
  }

  private static boolean defaulting(Change change) {
    return switch (change) {
      case AddColumnAttributeChange add -> add.attribute() instanceof DefaultAttribute;
      case DropColumnAttributeChange drop -> drop.attribute() instanceof DefaultAttribute;
      case AlterColumnAttributeChange alter -> alter.to() instanceof DefaultAttribute;
      default -> false;
    };
  }

  @Override
  public Command createSchema(String schema) {
    return new SqlServerCreateSchemaCommand(schema);
  }

  @Override
  public Command columnDefinition(Table definition, Column column) {
    return new SqlServerColumnDefinitionCommand(definition, column);
  }

  @Override
  public Command renameTable(TableName from, String to) {
    return new SqlServerRenameTableCommand(from, to);
  }

  @Override
  public Command moveTable(TableName from, String to) {
    return new SqlServerMoveTableCommand(from, to);
  }

  @Override
  public Command renameColumn(TableName table, String from, String to) {
    return new SqlServerRenameColumnCommand(table, from, to);
  }

  @Override
  public Command addColumn(TableName table, Column column) {
    return new SqlServerAddColumnCommand(table, column);
  }

  @Override
  public Command dropColumn(TableName table, String column) {
    return new SqlServerDropColumnCommand(table, column);
  }

  @Override
  public Command setDefault(TableName table, String column, DefaultAttribute value) {
    return new SqlServerSetDefaultCommand(table, column, value);
  }

  @Override
  public Command dropDefault(TableName table, String column) {
    return new SqlServerDropDefaultCommand(table, column);
  }

  @Override
  public Command addIdentity(TableName table, String column) {
    throw new SchemaException("SQL Server cannot add IDENTITY to the existing column " + table + "." + column
        + ". Add a new column or a new table instead.");
  }

  @Override
  public Command dropIdentity(TableName table, String column) {
    throw new SchemaException("SQL Server cannot remove IDENTITY from the existing column " + table + "." + column
        + ". Add a new column or a new table instead.");
  }

  @Override
  public Command dropIndex(TableName table, IndexAttribute index) {
    return new SqlServerDropIndexCommand(table, index);
  }

  @Override
  public Command foreignKeyConstraint(ForeignKeyAttribute key) {
    return new SqlServerForeignKeyConstraintCommand(key);
  }

  @Override
  public Command setColumnComment(TableName table, String column, CommentAttribute from, CommentAttribute to) {
    return new SqlServerSetColumnCommentCommand(table, column, from, to);
  }

  @Override
  public Command setTableComment(TableName table, CommentAttribute from, CommentAttribute to) {
    return new SqlServerSetTableCommentCommand(table, from, to);
  }
}
