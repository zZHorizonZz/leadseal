package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Type;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import java.util.Optional;

/**
 * Changes the type of a column and converts its values. The default of the column is dropped before the change and
 * set again after it.
 */
public class AlterColumnTypeCommand extends Command {

  private final TableName table;
  private final String column;
  private final Type from;
  private final Type to;

  public AlterColumnTypeCommand(TableName table, String column, Type from, Type to) {
    this.table = table;
    this.column = column;
    this.from = from;
    this.to = to;
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
   * Returns the column name.
   *
   * @return the column name
   */
  public String column() {
    return column;
  }

  /**
   * Returns the type before.
   *
   * @return the type before
   */
  public Type from() {
    return from;
  }

  /**
   * Returns the type after.
   *
   * @return the type after
   */
  public Type to() {
    return to;
  }

  @Override
  public void render(ScriptWriter sql) {
    Column after = sql.columnAfter(table, column);
    String type = sql.dialect().type(sql.tableAfter(table), after);
    Optional<DefaultAttribute> value = after.attribute(DefaultAttribute.class);
    if (value.isPresent()) {
      sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column).append(" DROP DEFAULT").end();
    }
    sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column).append(" TYPE " + type);
    after.collation(sql.dialect().name()).ifPresent(collation -> sql.append(" COLLATE ").name(collation));
    sql.append(" USING ").name(column).append("::" + type).end();
    value.ifPresent(present -> sql.render(sql.commands().setDefault(table, column, present)));
  }
}
