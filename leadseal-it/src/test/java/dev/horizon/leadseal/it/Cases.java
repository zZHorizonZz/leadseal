package dev.horizon.leadseal.it;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.protobuf.OptionsProto;
import dev.horizon.leadseal.protobuf.ProtoSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class Cases {

  private final Path root;
  private final Map<String, FileDescriptor> files = new LinkedHashMap<>();

  Cases(Path root, Path descriptors) throws IOException, DescriptorValidationException {
    this.root = root;
    ExtensionRegistry extensions = ExtensionRegistry.newInstance();
    OptionsProto.registerAllExtensions(extensions);
    for (FileDescriptorProto proto : FileDescriptorSet.parseFrom(Files.readAllBytes(descriptors), extensions)
        .getFileList()) {
      FileDescriptor[] dependencies = proto.getDependencyList().stream().map(files::get)
          .toArray(FileDescriptor[]::new);
      files.put(proto.getName(), FileDescriptor.buildFrom(proto, dependencies));
    }
  }

  Schema schema(String suite, String version) {
    String folder = suite + "/" + version + "/";
    List<FileDescriptor> chosen = files.entrySet().stream().filter(entry -> entry.getKey().startsWith(folder))
        .map(Map.Entry::getValue).toList();
    if (chosen.isEmpty()) {
      throw new IllegalArgumentException("The suite " + suite + " has no schema " + version + ".");
    }
    return new ProtoSchema().read(chosen);
  }

  List<Behavior> behaviors() throws IOException {
    List<Behavior> all = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(root)) {
      for (Path file : paths.filter(path -> path.toString().endsWith(".sql")).sorted().toList()) {
        all.add(Behavior.read(root, file));
      }
    }
    return all;
  }
}
