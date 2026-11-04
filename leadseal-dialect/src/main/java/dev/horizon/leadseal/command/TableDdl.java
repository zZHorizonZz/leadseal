package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.command.Command.CreateSchema;
import dev.horizon.leadseal.command.Command.CreateTable;
import dev.horizon.leadseal.command.Command.DropTable;
import dev.horizon.leadseal.command.Command.RenameConstraint;
import dev.horizon.leadseal.command.Command.RenameTable;
import java.util.ArrayList;
import java.util.List;

/** The statements of tables and namespaces. Standard SQL has no statement that moves a table to another namespace. */
public interface TableDdl {

  /**
   * Gives the statements that these statements are part of.
   *
   * @return the statements of the dialect
   */
  Ddl ddl();

  /**
   * Gives the commands that create a namespace if the database does not have it.
   *
   * @param namespace the namespace
   * @return the commands. The default is {@code CREATE SCHEMA IF NOT EXISTS}.
   */
  default List<Command> createSchema(String namespace) {
    return List.of(new CreateSchema(namespace));
  }

  /**
   * Gives the commands that create a table with its indexes and comments, but without its foreign keys.
   *
   * @param table the table
   * @param definition the table without its foreign keys
   * @return the commands
   */
  default List<Command> createTable(TableTransition table, Table definition) {
    TableName name = table.name();
    List<Command> all = new ArrayList<>(name.namespace().map(this::createSchema, List::of));
    List<Clause> elements = new ArrayList<>();
    definition.columns().forEach(column -> elements.add(ddl().column().definition(definition, column)));
    elements.add(new Clause.PrimaryKeyConstraint(definition.primaryKey()));
    definition.attributes(Check.class).forEach(check -> elements.add(new Clause.CheckConstraint(check)));
    all.add(new CreateTable(name, elements));
    definition.attributes(Index.class).forEach(index -> all.addAll(ddl().constraint().addIndex(table, index)));
    if (definition.attribute(Comment.class).isPresent()) {
      all.addAll(ddl().comment().commentTable(table));
    }
    definition.columns().stream().filter(column -> column.attribute(Comment.class).isPresent())
        .forEach(column -> all.addAll(ddl().comment().commentColumn(table.column(column.name()))));
    return all;
  }

  /**
   * Gives the commands that drop a table.
   *
   * @param table the table
   * @return the commands. The default is {@code DROP TABLE}.
   */
  default List<Command> dropTable(TableTransition table) {
    return List.of(new DropTable(table.name()));
  }

  /**
   * Gives the commands that rename a table, and its primary key if the name of the key follows the table.
   *
   * @param table the table, by its name before the migration
   * @param name the new name
   * @return the commands
   */
  default List<Command> renameTable(TableTransition table, String name) {
    String origin = table.origin().orElseThrow().primaryKey().name();
    String target = table.definition().primaryKey().name();
    List<Command> all = new ArrayList<>(List.of(new RenameTable(table.name(), name)));
    if (!origin.equals(target)) {
      all.add(new RenameConstraint(table.name().withName(name), origin, target));
    }
    return all;
  }

  /**
   * Gives the commands that move a table to another namespace and keep its rows.
   *
   * @param table the table, with its new name in its old namespace
   * @param namespace the new namespace
   * @return the commands
   */
  List<Command> moveTable(TableName table, Namespace.Name namespace);
}
