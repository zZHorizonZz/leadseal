package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.command.ColumnDefinitionCommand;
import java.util.regex.Pattern;

/**
 * A SQL Server column definition: the name, the type, the collation, {@code IDENTITY}, {@code NULL} or
 * {@code NOT NULL}, and a named default constraint. SQL Server writes collation names without quotes.
 */
class SqlServerColumnDefinitionCommand extends ColumnDefinitionCommand {

  private static final Pattern COLLATION = Pattern.compile("[A-Za-z0-9_]+");

  SqlServerColumnDefinitionCommand(Table definition, Column column) {
    super(definition, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    Column column = column();
    renderType(sql, column);
    if (column.attribute(IdentityAttribute.class).isPresent()) {
      sql.append(" IDENTITY(1, 1)");
    }
    sql.append(column.nullable() ? " NULL" : " NOT NULL");
    column.attribute(DefaultAttribute.class).ifPresent(value -> sql.append(" CONSTRAINT ")
        .name(new SqlServerDefaultName(definition().name(), column.name()).value())
        .append(" DEFAULT " + sql.dialect().defaultValue(column, value)));
  }

  /**
   * Renders the name, the type and the collation, the part of a definition that {@code ALTER COLUMN} repeats.
   *
   * @param sql the writer
   * @param column the column
   */
  void renderType(ScriptWriter sql, Column column) {
    sql.name(column.name()).append(" " + sql.dialect().type(definition(), column));
    column.collation(sql.dialect().name()).ifPresent(collation -> {
      if (!COLLATION.matcher(collation).matches()) {
        throw new SchemaException("The SQL Server collation '" + collation + "' is not a collation name.");
      }
      sql.append(" COLLATE " + collation);
    });
  }
}
