package dev.horizon.leadseal.protoc;

import dev.horizon.leadseal.ConfigurationException;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.migration.MigrationDirectory;
import dev.horizon.leadseal.migration.MigrationOptions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import picocli.CommandLine;

record GeneratorOptions(Path migrations, List<String> dialects, String prefix, boolean unsafe) {

  private static final Set<String> NAMES = Set.of("migrations", "dialect", "prefix", "allow_unsafe");

  GeneratorOptions {
    dialects = List.copyOf(dialects);
    if (dialects.isEmpty()) {
      throw new ConfigurationException("Set at least one dialect option, for example dialect=postgresql.");
    }
  }

  static GeneratorOptions of(String parameter, String... args) {
    if (args.length == 0) {
      return parse(parameter);
    }
    if (!parameter.isBlank()) {
      throw new ConfigurationException("Cannot specify both a protoc parameter and command line arguments.");
    }
    GeneratorCommand command = new GeneratorCommand();
    new CommandLine(command).parseArgs(args);
    return command.options();
  }

  static GeneratorOptions parse(String parameter) {
    Map<String, List<String>> values = new LinkedHashMap<>();
    for (String option : parameter.split(",")) {
      String[] parts = option.trim().split("=", 2);
      String name = parts[0].trim();
      if (name.isEmpty()) {
        continue;
      }
      if (parts.length != 2) {
        throw new ConfigurationException("The option '" + name + "' needs a value, for example " + name + "=...");
      }
      if (!NAMES.contains(name)) {
        throw new ConfigurationException("Unknown option '" + name + "'. The options are migrations, dialect, prefix"
            + " and allow_unsafe.");
      }
      values.computeIfAbsent(name, _ -> new ArrayList<>()).add(parts[1].trim());
    }
    List<String> migrations = values.getOrDefault("migrations", List.of());
    if (migrations.isEmpty()) {
      throw new ConfigurationException("Set the migrations option to the migration directory, for example"
          + " migrations=src/main/resources/db/migration.");
    }
    String unsafe = values.getOrDefault("allow_unsafe", List.of("false")).getLast();
    if (!unsafe.equals("true") && !unsafe.equals("false")) {
      throw new ConfigurationException("The option allow_unsafe is true or false, but is '" + unsafe + "'.");
    }
    return new GeneratorOptions(Path.of(migrations.getLast()), values.getOrDefault("dialect", List.of()),
        values.getOrDefault("prefix", List.of(MigrationOptions.DEFAULT_PREFIX)).getLast(),
        Boolean.parseBoolean(unsafe));
  }

  MigrationDirectory directory() {
    MigrationOptions options = MigrationOptions.defaults().withPrefix(prefix);
    return new MigrationDirectory(migrations, dialects.stream().map(Dialect::of).toList(),
        unsafe ? options.allowUnsafe() : options);
  }
}
