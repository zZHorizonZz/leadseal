package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.command.ColumnDefinitionCommand;
import java.util.regex.Pattern;

/**
 * A SQLite column definition: the name, the type, the collation, {@code NOT NULL} and the default. SQLite has no
 * identity keyword: an integer primary key of one column is the rowid.
 */
class SqliteColumnDefinitionCommand extends ColumnDefinitionCommand {

  private static final Pattern COLLATION = Pattern.compile("[A-Za-z0-9_]+");

  SqliteColumnDefinitionCommand(Table definition, Column column) {
    super(definition, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    Column column = column();
    sql.name(column.name()).append(" " + sql.dialect().type(definition(), column));
    column.collation(sql.dialect().name()).ifPresent(collation -> {
      if (!COLLATION.matcher(collation).matches()) {
        throw new SchemaException("The SQLite collation '" + collation + "' is not a collation name.");
      }
      sql.append(" COLLATE " + collation);
    });
    if (!column.nullable()) {
      sql.append(" NOT NULL");
    }
    column.attribute(DefaultAttribute.class)
        .ifPresent(value -> sql.append(" DEFAULT " + sql.dialect().defaultValue(column, value)));
  }
}
