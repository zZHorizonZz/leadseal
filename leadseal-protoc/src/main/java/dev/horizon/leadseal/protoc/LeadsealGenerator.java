package dev.horizon.leadseal.protoc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.MigrationFile;
import dev.horizon.leadseal.MigrationOptions;
import dev.horizon.leadseal.Migrations;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.protobuf.OptionsProto;
import dev.horizon.leadseal.protobuf.ProtoSchema;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;

/**
 * protoc-gen-leadseal: the protoc plugin that generates migration files from table messages.
 *
 * <p>
 * The plugin reads the migration history from the {@code migrations} directory and returns the files of the new
 * migration. Point the plugin's output to the same directory, for example
 * {@code --leadseal_out=db/migration --leadseal_opt=migrations=db/migration,dialect=postgresql}. Repeat
 * {@code dialect} for each database. The {@code prefix} option sets the prefix of the temporary objects of migrations,
 * {@code leadseal_} by default, and {@code prefix=} removes it.
 */
public final class LeadsealGenerator {

  private static final List<CodeGeneratorResponse.Feature> SUPPORTED_FEATURES = List.of(
      CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL,
      CodeGeneratorResponse.Feature.FEATURE_SUPPORTS_EDITIONS);

  private LeadsealGenerator() {
  }

  public static void main(String[] args) {
    try {
      ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
      OptionsProto.registerAllExtensions(extensionRegistry);

      CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(System.in.readAllBytes(), extensionRegistry);

      if (args.length > 0) {
        if (!request.getParameter().isBlank()) {
          throw new IllegalArgumentException("Cannot specify both a protoc parameter and command line arguments.");
        }
        LeadsealGeneratorCommand command = new LeadsealGeneratorCommand();
        new CommandLine(command).parseArgs(args);
        request = request.toBuilder().setParameter(command.toString()).build();
      }

      processRequest(request).writeTo(System.out);
    } catch (Exception e) {
      try {
        CodeGeneratorResponse.newBuilder().setError("Plugin execution failed: " + e.getMessage()).build()
            .writeTo(System.out);
      } catch (IOException ioException) {
        System.err.println("Failed to write error response: " + ioException.getMessage());
        System.exit(1);
      }
    }
  }

  /**
   * Processes a protoc request: reads the table messages of the files to generate and returns the files of the new
   * migration.
   *
   * @param request the request, parsed with the leadseal extensions
   * @return the response, or a response with an error
   */
  public static CodeGeneratorResponse processRequest(CodeGeneratorRequest request) {
    try {
      GenerationOptions options = parseOptions(request.getParameter());
      List<FileDescriptor> files = filesToGenerate(request);

      Migrations migrations = new Migrations(options.migrations(), options.dialects().stream().map(Dialect::named)
          .toList(), MigrationOptions.defaults().withPrefix(options.prefix()));
      Migrations.Result result = migrations.plan(new ProtoSchema().read(files));

      int featureMask = SUPPORTED_FEATURES.stream().mapToInt(CodeGeneratorResponse.Feature::getNumber)
          .reduce(CodeGeneratorResponse.Feature.FEATURE_NONE_VALUE, (l, r) -> l | r);

      CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder()
          .setSupportedFeatures(featureMask)
          .setMinimumEdition(DescriptorProtos.Edition.EDITION_PROTO2_VALUE)
          .setMaximumEdition(DescriptorProtos.Edition.EDITION_2024_VALUE);
      for (MigrationFile file : result.files()) {
        response.addFile(CodeGeneratorResponse.File.newBuilder()
            .setName(file.path().toString().replace('\\', '/'))
            .setContent(file.content()));
      }
      return response.build();
    } catch (SchemaException | DescriptorValidationException e) {
      return CodeGeneratorResponse.newBuilder().setError("Migration generation failed: " + e.getMessage()).build();
    }
  }

  /**
   * The plugin options.
   *
   * @param migrations the migration directory
   * @param dialects the dialect names
   * @param prefix the prefix of the temporary objects of migrations
   */
  record GenerationOptions(Path migrations, List<String> dialects, String prefix) {
  }

  static GenerationOptions parseOptions(String parameter) {
    Path migrations = null;
    List<String> dialects = new ArrayList<>();
    String prefix = MigrationOptions.DEFAULT_PREFIX;

    for (String param : parameter.split(",")) {
      String[] parts = param.trim().split("=", 2);
      if (parts[0].isEmpty()) {
        continue;
      }
      if (parts.length != 2) {
        throw new SchemaException("The option '" + parts[0] + "' needs a value, for example " + parts[0] + "=...");
      }
      switch (parts[0].trim()) {
        case "migrations" -> migrations = Path.of(parts[1].trim());
        case "dialect" -> dialects.add(parts[1].trim());
        case "prefix" -> prefix = parts[1].trim();
        default -> throw new SchemaException("Unknown option '" + parts[0].trim() + "'. The options are migrations,"
            + " dialect and prefix.");
      }
    }

    if (migrations == null) {
      throw new SchemaException("Set the migrations option to the migration directory, for example"
          + " migrations=src/main/resources/db/migration.");
    }
    if (dialects.isEmpty()) {
      throw new SchemaException("Set at least one dialect option, for example dialect=postgresql.");
    }
    return new GenerationOptions(migrations, dialects, prefix);
  }

  private static List<FileDescriptor> filesToGenerate(CodeGeneratorRequest request)
      throws DescriptorValidationException {
    Map<String, FileDescriptor> built = new HashMap<>();
    for (DescriptorProtos.FileDescriptorProto proto : request.getProtoFileList()) {
      FileDescriptor[] dependencies = proto.getDependencyList().stream().map(built::get)
          .toArray(FileDescriptor[]::new);
      built.put(proto.getName(), Descriptors.FileDescriptor.buildFrom(proto, dependencies));
    }
    return request.getFileToGenerateList().stream().map(built::get).toList();
  }
}
