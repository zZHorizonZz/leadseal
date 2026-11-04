package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.command.Command.AddColumn;
import dev.horizon.leadseal.command.Command.AlterColumn;
import dev.horizon.leadseal.command.Command.DropColumn;
import dev.horizon.leadseal.command.Command.FillNulls;
import dev.horizon.leadseal.command.Command.RenameColumn;
import java.util.ArrayList;
import java.util.List;

/** The statements of columns. Standard SQL has no statement that changes the collation of a column. */
public interface ColumnDdl {

  /**
   * Gives the statements that these statements are part of.
   *
   * @return the statements of the dialect
   */
  Ddl ddl();

  /**
   * Gives the definition of a column in {@code CREATE TABLE} and {@code ADD COLUMN}.
   *
   * @param table the table of the column
   * @param column the column
   * @return the clause. The default is the standard definition.
   */
  default Clause definition(Table table, Column column) {
    return new Clause.ColumnDefinition(table, column);
  }

  /**
   * Gives the commands that add a column. A required column without a default gets the zero of its type in the rows
   * that the table has.
   *
   * @param column the column
   * @return the commands
   */
  default List<Command> addColumn(ColumnTransition column) {
    TableName name = column.table().name();
    Column definition = column.definition();
    List<Command> all = new ArrayList<>();
    if (definition.needsFill()) {
      Column filled = definition.with(new LiteralDefault(definition.type().zero()));
      all.add(new AddColumn(name, definition(column.table().definition(), filled)));
      all.add(new AlterColumn(name, definition.name(), "DROP DEFAULT"));
    } else {
      all.add(new AddColumn(name, definition(column.table().definition(), definition)));
    }
    if (definition.attribute(Comment.class).isPresent()) {
      all.addAll(ddl().comment().commentColumn(column));
    }
    return all;
  }

  /**
   * Gives the commands that drop a column.
   *
   * @param column the column, by its name before the migration
   * @return the commands. The default is {@code DROP COLUMN}.
   */
  default List<Command> dropColumn(ColumnTransition column) {
    return List.of(new DropColumn(column.table().name(), column.name()));
  }

  /**
   * Gives the commands that rename a column.
   *
   * @param column the column, by its new name
   * @param origin the name of the column before the migration
   * @return the commands. The default is {@code RENAME COLUMN}.
   */
  default List<Command> renameColumn(ColumnTransition column, String origin) {
    return List.of(new RenameColumn(column.table().name(), origin, column.name()));
  }

  /**
   * Gives the commands that change the type of a column and keep its values.
   *
   * @param column the column
   * @return the commands. The default is {@code SET DATA TYPE}, which leaves the conversion of the values to the
   * database.
   */
  default List<Command> alterColumnType(ColumnTransition column) {
    String type = ddl().dialect().type(column.table().definition(), column.definition());
    return List.of(new AlterColumn(column.table().name(), column.name(), "SET DATA TYPE " + type));
  }

  /**
   * Gives the commands that make a column required. The rows without a value get its default, or the zero of its
   * type.
   *
   * @param column the column
   * @return the commands. The default fills the nulls and then sets {@code NOT NULL}.
   */
  default List<Command> setNotNull(ColumnTransition column) {
    return List.of(new FillNulls(column.table().name(), column.definition()),
        new AlterColumn(column.table().name(), column.name(), "SET NOT NULL"));
  }

  /**
   * Gives the commands that make a column optional.
   *
   * @param column the column
   * @return the commands. The default drops {@code NOT NULL}.
   */
  default List<Command> dropNotNull(ColumnTransition column) {
    return List.of(new AlterColumn(column.table().name(), column.name(), "DROP NOT NULL"));
  }

  /**
   * Gives the commands that set the default of a column to the default that it has after the migration.
   *
   * @param column the column
   * @return the commands. The default is {@code SET DEFAULT}.
   */
  default List<Command> setDefault(ColumnTransition column) {
    Column definition = column.definition();
    String value = definition.attribute(Default.class).orElseThrow().sql(ddl().dialect(), definition);
    return List.of(new AlterColumn(column.table().name(), column.name(), "SET DEFAULT " + value));
  }

  /**
   * Gives the commands that drop the default of a column.
   *
   * @param column the column
   * @return the commands. The default is {@code DROP DEFAULT}.
   */
  default List<Command> dropDefault(ColumnTransition column) {
    return List.of(new AlterColumn(column.table().name(), column.name(), "DROP DEFAULT"));
  }

  /**
   * Gives the commands that make the database generate the values of a column that has rows already.
   *
   * @param column the column
   * @return the commands. The default adds {@code GENERATED BY DEFAULT AS IDENTITY}.
   */
  default List<Command> addIdentity(ColumnTransition column) {
    return List.of(new AlterColumn(column.table().name(), column.name(), "ADD GENERATED BY DEFAULT AS IDENTITY"));
  }

  /**
   * Gives the commands that stop the database from generating the values of a column.
   *
   * @param column the column
   * @return the commands. The default drops {@code IDENTITY}.
   */
  default List<Command> dropIdentity(ColumnTransition column) {
    return List.of(new AlterColumn(column.table().name(), column.name(), "DROP IDENTITY"));
  }

  /**
   * Gives the commands that give a column the collation that it has after the migration in this dialect, or the
   * collation of the database if it has none.
   *
   * @param column the column
   * @param collation the collation that changes, of this or of another dialect
   * @return the commands, or none for a collation of another dialect
   */
  List<Command> collate(ColumnTransition column, Collation collation);
}
