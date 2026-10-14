package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;

/**
 * A T-SQL variable of one statement. Flyway sends statements that declare variables in one batch, where each variable
 * name must be unique, so the name contains the position of the statement in the script.
 *
 * @param name the variable name, for example {@code @leadseal_default_3}
 */
record SqlServerVariable(String name) {

  /**
   * Returns the variable of the current statement of a writer.
   *
   * @param sql the writer
   * @param purpose what the variable holds, such as {@code default}
   * @return the variable
   */
  static SqlServerVariable of(ScriptWriter sql, String purpose) {
    return new SqlServerVariable("@" + sql.internal(purpose + "_" + sql.position()));
  }
}
