package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * The comment of a table or a column.
 *
 * @param text the comment
 */
public record Comment(String text) implements TableAttribute, ColumnAttribute {

  public Comment {
    if (text == null || text.isBlank()) {
      throw new InvalidSchemaException("A comment has no text.");
    }
  }

  @Override
  public Object key() {
    return Comment.class;
  }

  @Override
  public String summary(String table) {
    return table + "_comment";
  }

  @Override
  public void validate(Table table) {
  }

  @Override
  public void validate(Column column) {
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.COMMENT_TABLES;
  }

  @Override
  public boolean alterable() {
    return true;
  }

  @Override
  public List<Command> add(Ddl ddl, ColumnTransition column) {
    return ddl.comment().commentColumn(column);
  }

  @Override
  public List<Command> drop(Ddl ddl, ColumnTransition column) {
    return ddl.comment().commentColumn(column);
  }

  @Override
  public List<Command> add(Ddl ddl, TableTransition table) {
    return ddl.comment().commentTable(table);
  }

  @Override
  public List<Command> drop(Ddl ddl, TableTransition table) {
    return ddl.comment().commentTable(table);
  }

  @Override
  public List<Command> alter(Ddl ddl, TableTransition table) {
    return ddl.comment().commentTable(table);
  }
}
