package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.change.Change;

/**
 * The comment of a table or a column.
 *
 * @param text the comment
 */
public record CommentAttribute(String text) implements TableAttribute, ColumnAttribute {

  public CommentAttribute {
    if (text == null || text.isBlank()) {
      throw new SchemaException("A comment has no text.");
    }
  }

  @Override
  public Object key() {
    return CommentAttribute.class;
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
}
