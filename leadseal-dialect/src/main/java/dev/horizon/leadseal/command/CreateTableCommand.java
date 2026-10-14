package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;

/**
 * {@code CREATE TABLE} with the columns, the primary key and the checks of a table, followed by its indexes and comments.
 * A table in a database schema creates the schema first.
 */
public class CreateTableCommand extends Command {

  private final TableName table;
  private final Table definition;

  public CreateTableCommand(TableName table, Table definition) {
    this.table = table;
    this.definition = definition;
  }

  /**
   * Returns the table.
   *
   * @return the table
   */
  public TableName table() {
    return table;
  }

  /**
   * Returns the definition of the table.
   *
   * @return the definition of the table
   */
  public Table definition() {
    return definition;
  }

  @Override
  public void render(ScriptWriter sql) {
    CommandFactory commands = sql.commands();
    if (table.namespace() != null) {
      sql.render(commands.createSchema(table.namespace())).end();
    }
    sql.append("CREATE TABLE ").table(table).append(" (");
    for (Column column : definition.columns()) {
      sql.render(commands.columnDefinition(definition, column)).append(", ");
    }
    PrimaryKeyAttribute key = definition.primaryKey();
    sql.append("CONSTRAINT ").name(key.name()).append(" PRIMARY KEY (").names(key.columns()).append(")");
    for (CheckAttribute check : definition.attributes(CheckAttribute.class)) {
      sql.append(", ").render(commands.checkConstraint(check));
    }
    sql.append(")").end();
    for (IndexAttribute index : definition.attributes(IndexAttribute.class)) {
      sql.render(commands.addIndex(table, index)).end();
    }
    definition.attribute(CommentAttribute.class)
        .ifPresent(comment -> sql.render(commands.setTableComment(table, null, comment)).end());
    for (Column column : definition.columns()) {
      column.attribute(CommentAttribute.class)
          .ifPresent(comment -> sql.render(commands.setColumnComment(table, column.name(), null, comment)).end());
    }
  }
}
