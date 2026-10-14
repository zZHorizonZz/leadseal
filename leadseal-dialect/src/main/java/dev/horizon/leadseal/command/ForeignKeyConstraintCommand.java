package dev.horizon.leadseal.command;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;

/**
 * The definition of a foreign key constraint: {@code CONSTRAINT name FOREIGN KEY (...) REFERENCES table (...)}
 * with its {@code ON DELETE} and {@code ON UPDATE} actions.
 */
public class ForeignKeyConstraintCommand extends Command {

  private final ForeignKeyAttribute key;

  public ForeignKeyConstraintCommand(ForeignKeyAttribute key) {
    this.key = key;
  }

  /**
   * Returns the foreign key.
   *
   * @return the foreign key
   */
  public ForeignKeyAttribute key() {
    return key;
  }

  @Override
  public void render(ScriptWriter sql) {
    sql.append("CONSTRAINT ").name(key.name()).append(" FOREIGN KEY (").names(key.columns()).append(") REFERENCES ")
        .table(key.table()).append(" (").names(key.referencedColumns()).append(")")
        .append(action(" ON DELETE", key.onDelete())).append(action(" ON UPDATE", key.onUpdate()));
  }

  /**
   * Returns the SQL of an action, or an empty text for {@code NO ACTION}, the default.
   *
   * @param event {@code ON DELETE} or {@code ON UPDATE}, with a leading space
   * @param action the action
   * @return the SQL
   */
  protected String action(String event, ForeignKeyAttribute.Action action) {
    return switch (action) {
      case NO_ACTION -> "";
      case RESTRICT -> event + " RESTRICT";
      case CASCADE -> event + " CASCADE";
      case SET_NULL -> event + " SET NULL";
    };
  }
}
