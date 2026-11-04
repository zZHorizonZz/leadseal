package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;

record SqlServerDropDefault(TableName table, String column) implements Command {

  @Override
  public void render(ScriptWriter script) {
    Dialect dialect = script.dialect();
    String path = dialect.quote(table);
    String statement = SqlServerVariable.of(script, "default").name();
    script.append("DECLARE " + statement + " nvarchar(max) = (SELECT "
        + dialect.string("ALTER TABLE " + path + " DROP CONSTRAINT ") + " + QUOTENAME(d.name) FROM"
        + " sys.default_constraints d JOIN sys.columns c ON c.object_id = d.parent_object_id AND c.column_id ="
        + " d.parent_column_id WHERE d.parent_object_id = OBJECT_ID(" + dialect.string(path) + ") AND c.name = "
        + dialect.string(column) + ") IF " + statement + " IS NOT NULL EXEC(" + statement + ")");
  }
}
