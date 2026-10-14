package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.command.Command;

/**
 * The {@code CREATE TABLE} statement of SQLite: the columns, the primary key, the checks and the foreign keys inline,
 * as SQLite cannot add constraints to an existing table.
 */
class SqliteTableStatementCommand extends Command {

  private final Table definition;
  private final TableName name;

  SqliteTableStatementCommand(Table definition, TableName name) {
    this.definition = definition;
    this.name = name;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("CREATE TABLE ").table(name).append(" (");
    for (Column column : definition.columns()) {
      sql.render(sql.commands().columnDefinition(definition, column)).append(", ");
    }
    PrimaryKeyAttribute key = definition.primaryKey();
    sql.append("CONSTRAINT ").name(key.name()).append(" PRIMARY KEY (").names(key.columns()).append(")");
    for (CheckAttribute check : definition.attributes(CheckAttribute.class)) {
      sql.append(", ").render(sql.commands().checkConstraint(check));
    }
    for (ForeignKeyAttribute foreign : definition.attributes(ForeignKeyAttribute.class)) {
      sql.append(", ").render(sql.commands().foreignKeyConstraint(foreign));
    }
    sql.append(")");
  }
}
