package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Type;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.attribute.TableAttribute;
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
import dev.horizon.leadseal.change.MoveTableChange;
import dev.horizon.leadseal.change.RenameColumnChange;
import dev.horizon.leadseal.change.RenameTableChange;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the commands of a dialect. This class creates the commands of standard SQL.
 *
 * <p>
 * A dialect extends this class and overrides the methods of the commands that its database writes differently, so
 * that they return subclasses of the standard commands. A dialect that has to write several changes together, for
 * example one statement for all changes of a column, overrides {@link #plan}.
 */
public class CommandFactory {

  /**
   * Returns the commands of a migration.
   *
   * @param changes the changes, in the order they run
   * @param sql the writer, with the schemas before and after the migration
   * @return the commands, in the order they run
   */
  public List<Command> plan(List<Change> changes, ScriptWriter sql) {
    List<Command> commands = new ArrayList<>();
    for (Change change : changes) {
      commands.addAll(commands(change, sql));
    }
    return commands;
  }

  /**
   * Returns the commands of one change.
   *
   * @param change the change
   * @param sql the writer
   * @return the commands
   */
  protected List<Command> commands(Change change, ScriptWriter sql) {
    return switch (change) {
      case CreateTableChange create -> List.of(createTable(create.table(), create.definition()));
      case DropTableChange drop -> List.of(dropTable(drop.table()));
      case RenameTableChange rename -> List.of(renameTable(rename.from(), rename.to()));
      case MoveTableChange move -> List.of(moveTable(move.from(), move.to()));
      case AddColumnChange add -> List.of(addColumn(add.table(), add.column()));
      case DropColumnChange drop -> List.of(dropColumn(drop.table(), drop.column()));
      case RenameColumnChange rename -> List.of(renameColumn(rename.table(), rename.from(), rename.to()));
      case AlterColumnTypeChange alter -> List.of(alterColumnType(alter.table(), alter.column(), alter.from(),
          alter.to()));
      case AddColumnAttributeChange add -> columnAttribute(add.table(), add.column(), null, add.attribute(), sql);
      case DropColumnAttributeChange drop -> columnAttribute(drop.table(), drop.column(), drop.attribute(), null, sql);
      case AlterColumnAttributeChange alter -> columnAttribute(alter.table(), alter.column(), alter.from(),
          alter.to(), sql);
      case AddTableAttributeChange add -> tableAttribute(add.table(), null, add.attribute());
      case DropTableAttributeChange drop -> tableAttribute(drop.table(), drop.attribute(), null);
      case AlterTableAttributeChange alter -> tableAttribute(alter.table(), alter.from(), alter.to());
    };
  }

  /**
   * Returns the commands that add, drop or change an attribute of a column.
   *
   * @param table the table
   * @param column the column name
   * @param from the attribute before, or {@code null} if it is added
   * @param to the attribute after, or {@code null} if it is dropped
   * @param sql the writer
   * @return the commands
   */
  protected List<Command> columnAttribute(TableName table, String column, ColumnAttribute from, ColumnAttribute to,
      ScriptWriter sql) {
    return switch (to != null ? to : from) {
      case NotNullAttribute notNull -> List.of(to != null ? setNotNull(table, column) : dropNotNull(table, column));
      case DefaultAttribute value -> List.of(to != null ? setDefault(table, column, (DefaultAttribute) to)
          : dropDefault(table, column));
      case IdentityAttribute identity -> List.of(to != null ? addIdentity(table, column)
          : dropIdentity(table, column));
      case CollationAttribute collation -> collation.dialect().equals(sql.dialect().name())
          ? List.of(setCollation(table, column, to == null ? null : ((CollationAttribute) to).name()))
          : List.of();
      case CommentAttribute comment -> List.of(setColumnComment(table, column, (CommentAttribute) from,
          (CommentAttribute) to));
    };
  }

  /**
   * Returns the commands that add, drop or change an attribute of a table.
   *
   * @param table the table
   * @param from the attribute before, or {@code null} if it is added
   * @param to the attribute after, or {@code null} if it is dropped
   * @return the commands
   */
  protected List<Command> tableAttribute(TableName table, TableAttribute from, TableAttribute to) {
    return switch (to != null ? to : from) {
      case PrimaryKeyAttribute key -> List.of(alterPrimaryKey(table, (PrimaryKeyAttribute) from,
          (PrimaryKeyAttribute) to));
      case IndexAttribute index -> List.of(to != null ? addIndex(table, index) : dropIndex(table, index));
      case CheckAttribute check -> List.of(to != null ? addCheck(table, check) : dropCheck(table, check));
      case ForeignKeyAttribute key -> List.of(to != null ? addForeignKey(table, key) : dropForeignKey(table, key));
      case CommentAttribute comment -> List.of(setTableComment(table, (CommentAttribute) from,
          (CommentAttribute) to));
    };
  }

  public Command createSchema(String schema) {
    return new CreateSchemaCommand(schema);
  }

  public Command createTable(TableName table, Table definition) {
    return new CreateTableCommand(table, definition);
  }

  public Command dropTable(TableName table) {
    return new DropTableCommand(table);
  }

  public Command renameTable(TableName from, String to) {
    return new RenameTableCommand(from, to);
  }

  public Command moveTable(TableName from, String to) {
    return new MoveTableCommand(from, to);
  }

  public Command columnDefinition(Table definition, Column column) {
    return new ColumnDefinitionCommand(definition, column);
  }

  public Command addColumn(TableName table, Column column) {
    return new AddColumnCommand(table, column);
  }

  public Command dropColumn(TableName table, String column) {
    return new DropColumnCommand(table, column);
  }

  public Command renameColumn(TableName table, String from, String to) {
    return new RenameColumnCommand(table, from, to);
  }

  public Command alterColumnType(TableName table, String column, Type from, Type to) {
    return new AlterColumnTypeCommand(table, column, from, to);
  }

  public Command setNotNull(TableName table, String column) {
    return new SetNotNullCommand(table, column);
  }

  public Command dropNotNull(TableName table, String column) {
    return new DropNotNullCommand(table, column);
  }

  public Command setDefault(TableName table, String column, DefaultAttribute value) {
    return new SetDefaultCommand(table, column, value);
  }

  public Command dropDefault(TableName table, String column) {
    return new DropDefaultCommand(table, column);
  }

  public Command addIdentity(TableName table, String column) {
    return new AddIdentityCommand(table, column);
  }

  public Command dropIdentity(TableName table, String column) {
    return new DropIdentityCommand(table, column);
  }

  public Command setCollation(TableName table, String column, String collation) {
    return new SetCollationCommand(table, column, collation);
  }

  public Command setColumnComment(TableName table, String column, CommentAttribute from, CommentAttribute to) {
    return new SetColumnCommentCommand(table, column, from, to);
  }

  public Command setTableComment(TableName table, CommentAttribute from, CommentAttribute to) {
    return new SetTableCommentCommand(table, from, to);
  }

  public Command alterPrimaryKey(TableName table, PrimaryKeyAttribute from, PrimaryKeyAttribute to) {
    return new AlterPrimaryKeyCommand(table, from, to);
  }

  public Command addIndex(TableName table, IndexAttribute index) {
    return new AddIndexCommand(table, index);
  }

  public Command dropIndex(TableName table, IndexAttribute index) {
    return new DropIndexCommand(table, index);
  }

  public Command checkConstraint(CheckAttribute check) {
    return new CheckConstraintCommand(check);
  }

  public Command addCheck(TableName table, CheckAttribute check) {
    return new AddCheckCommand(table, check);
  }

  public Command dropCheck(TableName table, CheckAttribute check) {
    return new DropCheckCommand(table, check);
  }

  public Command foreignKeyConstraint(ForeignKeyAttribute key) {
    return new ForeignKeyConstraintCommand(key);
  }

  public Command addForeignKey(TableName table, ForeignKeyAttribute key) {
    return new AddForeignKeyCommand(table, key);
  }

  public Command dropForeignKey(TableName table, ForeignKeyAttribute key) {
    return new DropForeignKeyCommand(table, key);
  }
}
