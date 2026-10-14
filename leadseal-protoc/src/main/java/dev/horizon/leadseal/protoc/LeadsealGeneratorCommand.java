package dev.horizon.leadseal.protoc;

import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(name = "protoc-gen-leadseal", mixinStandardHelpOptions = true, description = "Generates SQL migrations from protobuf table messages.")
public class LeadsealGeneratorCommand {

  @CommandLine.Option(names = {
      "--migrations" }, required = true, description = "The migration directory. Point the plugin output to the same directory.")
  public String migrations;

  @CommandLine.Option(names = {
      "--dialect" }, required = true, description = "A database to generate SQL for: postgresql, mariadb, sqlserver or sqlite. Repeat for each"
          + " database.")
  public List<String> dialects = new ArrayList<>();

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("migrations=").append(migrations);
    for (String dialect : dialects) {
      sb.append(",dialect=").append(dialect);
    }

    return sb.toString();
  }
}
