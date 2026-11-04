package dev.horizon.leadseal.protoc;

import dev.horizon.leadseal.migration.MigrationOptions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "protoc-gen-leadseal", mixinStandardHelpOptions = true, description = "Generates SQL migrations from protobuf table messages.")
final class GeneratorCommand {

  @Option(names = "--migrations", required = true, description = "The migration directory. Point the plugin output to the same directory.")
  private Path migrations;

  @Option(names = "--dialect", required = true, description = "A database to generate SQL for: postgresql, mariadb,"
      + " sqlserver or sqlite. Repeat for each database.")
  private List<String> dialects = new ArrayList<>();

  @Option(names = "--prefix", defaultValue = MigrationOptions.DEFAULT_PREFIX, description = "The prefix of the"
      + " temporary objects of migrations. The default is leadseal_. Set it to an empty text to remove it.")
  private String prefix;

  GeneratorOptions options() {
    return new GeneratorOptions(migrations, dialects, prefix);
  }
}
