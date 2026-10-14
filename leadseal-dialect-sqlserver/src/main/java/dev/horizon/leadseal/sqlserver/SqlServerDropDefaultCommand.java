package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.DropDefaultCommand;

/**
 * Drops the default constraint of a column, whatever its name. Renames do not rename default constraints, so the
 * command looks the name up.
 */
class SqlServerDropDefaultCommand extends DropDefaultCommand {

  SqlServerDropDefaultCommand(TableName table, String column) {
    super(table, column);
  }

  @Override
  public void render(ScriptWriter sql) {
    String table = sql.dialect().qualified(table());
    String statement = SqlServerVariable.of(sql, "default").name();
    sql.append("DECLARE " + statement + " nvarchar(max) = (SELECT "
        + sql.dialect().string("ALTER TABLE " + table + " DROP CONSTRAINT ") + " + QUOTENAME(d.name) FROM"
        + " sys.default_constraints d JOIN sys.columns c ON c.object_id = d.parent_object_id AND c.column_id ="
        + " d.parent_column_id WHERE d.parent_object_id = OBJECT_ID(" + sql.dialect().string(table) + ") AND c.name = "
        + sql.dialect().string(column()) + ") IF " + statement + " IS NOT NULL EXEC(" + statement + ")");
  }
}
