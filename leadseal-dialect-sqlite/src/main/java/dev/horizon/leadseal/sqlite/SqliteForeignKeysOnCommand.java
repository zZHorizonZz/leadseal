package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.command.Command;

/** Checks the foreign keys after rebuilds, fails if a row breaks one, and turns foreign keys on again. */
class SqliteForeignKeysOnCommand extends Command {

  @Override
  public void render(ScriptWriter sql) {
    sql.append("INSERT INTO ").name(sql.internal("check"))
        .append(" (violations) SELECT COUNT(*) FROM pragma_foreign_key_check").end();
    sql.append("DROP TABLE ").name(sql.internal("check")).end();
    sql.append("PRAGMA foreign_keys = ON");
  }
}
