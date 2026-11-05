package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.command.Command;

record SqlServerRename(String path, String name, String kind) implements Command {

  @Override
  public void render(ScriptWriter script) {
    script.append("EXEC sp_rename " + script.dialect().string(path) + ", " + script.dialect().string(name) + kind);
  }
}
