package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.command.Command;

/**
 * Turns foreign keys off before rebuilds of referenced tables, so that dropping the old table does not run the
 * {@code ON DELETE} actions of the foreign keys that reference it. SQLite allows this only outside a transaction, so
 * the script runs outside one. The script fails here if foreign keys are still on.
 */
class SqliteForeignKeysOffCommand extends Command {

  @Override
  public void render(ScriptWriter sql) {
    sql.outsideTransaction();
    sql.append("PRAGMA foreign_keys = OFF").end();
    sql.append("CREATE TEMP TABLE ").name(sql.internal("check")).append(" (foreign_keys INTEGER CONSTRAINT"
        + " foreign_keys_must_be_off"
        + " CHECK (foreign_keys = 0), violations INTEGER CONSTRAINT foreign_keys_must_hold CHECK (violations = 0))")
        .end();
    sql.append("INSERT INTO ").name(sql.internal("check"))
        .append(" (foreign_keys) SELECT foreign_keys FROM pragma_foreign_keys");
  }
}
