package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.command.AddIndexCommand;

/** {@code CREATE INDEX}. MariaDB has no partial indexes, so an index with a condition fails. */
class MariaDbAddIndexCommand extends AddIndexCommand {

  MariaDbAddIndexCommand(TableName table, IndexAttribute index) {
    super(table, index);
  }

  @Override
  public void render(ScriptWriter sql) {
    if (index().where() != null) {
      throw new SchemaException("MariaDB has no partial indexes, so it cannot create the index '" + index().name()
          + "' with the condition " + index().where() + ".");
    }
    super.render(sql);
  }
}
