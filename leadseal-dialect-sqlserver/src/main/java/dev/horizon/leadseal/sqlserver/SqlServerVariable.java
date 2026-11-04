package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;

record SqlServerVariable(String name) {

  static SqlServerVariable of(ScriptWriter script, String purpose) {
    return new SqlServerVariable("@" + script.internal(purpose + "_" + script.position()));
  }
}
