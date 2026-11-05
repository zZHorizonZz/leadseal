package dev.horizon.leadseal.protoc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.migration.MigrationFile;
import dev.horizon.leadseal.migration.MigrationPlan;
import dev.horizon.leadseal.protobuf.OptionsProto;
import dev.horizon.leadseal.protobuf.ProtoSchema;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LeadsealGenerator {

  private static final int FEATURES = CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL_VALUE
      | CodeGeneratorResponse.Feature.FEATURE_SUPPORTS_EDITIONS_VALUE;

  public static void main(String[] args) {
    try {
      ExtensionRegistry extensions = ExtensionRegistry.newInstance();
      OptionsProto.registerAllExtensions(extensions);
      CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(System.in.readAllBytes(), extensions);
      new LeadsealGenerator().generate(request, args).writeTo(System.out);
    } catch (Exception exception) {
      try {
        CodeGeneratorResponse.newBuilder().setError("Plugin execution failed: " + exception.getMessage()).build()
            .writeTo(System.out);
      } catch (IOException failure) {
        System.err.println("Failed to write error response: " + failure.getMessage());
        System.exit(1);
      }
    }
  }

  CodeGeneratorResponse generate(CodeGeneratorRequest request, String... args) {
    try {
      GeneratorOptions options = GeneratorOptions.of(request.getParameter(), args);
      CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder()
          .setSupportedFeatures(FEATURES)
          .setMinimumEdition(DescriptorProtos.Edition.EDITION_PROTO2_VALUE)
          .setMaximumEdition(DescriptorProtos.Edition.EDITION_2024_VALUE);
      MigrationPlan plan = options.directory().plan(new ProtoSchema().read(files(request)));
      plan.migration().ifPresent(folder -> plan.warnings().forEach(warning -> System.err.println(
          "leadseal: The migration " + folder + " " + warning + ".")));
      for (MigrationFile file : plan.files()) {
        response.addFile(CodeGeneratorResponse.File.newBuilder()
            .setName(file.path().toString().replace('\\', '/'))
            .setContent(file.content()));
      }
      return response.build();
    } catch (SchemaException | DescriptorValidationException exception) {
      return CodeGeneratorResponse.newBuilder().setError("Migration generation failed: " + exception.getMessage())
          .build();
    }
  }

  private List<FileDescriptor> files(CodeGeneratorRequest request) throws DescriptorValidationException {
    Map<String, FileDescriptor> descriptors = new HashMap<>();
    for (FileDescriptorProto proto : request.getProtoFileList()) {
      FileDescriptor[] dependencies = proto.getDependencyList().stream().map(descriptors::get)
          .toArray(FileDescriptor[]::new);
      descriptors.put(proto.getName(), FileDescriptor.buildFrom(proto, dependencies));
    }
    return request.getFileToGenerateList().stream().map(descriptors::get).toList();
  }
}
