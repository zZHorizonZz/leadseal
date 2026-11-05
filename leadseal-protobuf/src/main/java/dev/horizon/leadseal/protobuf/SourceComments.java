package dev.horizon.leadseal.protobuf;

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class SourceComments {

  private static final int MESSAGE_TYPE = 4;
  private static final int NESTED_TYPE = 3;
  private static final int FIELD = 2;

  private final Map<List<Integer>, String> comments = new HashMap<>();

  SourceComments(FileDescriptor file) {
    for (SourceCodeInfo.Location location : file.toProto().getSourceCodeInfo().getLocationList()) {
      String text = location.getLeadingComments().lines().map(String::strip).collect(Collectors.joining("\n"))
          .strip();
      if (!text.isEmpty()) {
        comments.put(List.copyOf(location.getPathList()), text);
      }
    }
  }

  Optional<String> of(Descriptor message) {
    return Optional.ofNullable(comments.get(path(message)));
  }

  Optional<String> of(FieldDescriptor field) {
    List<Integer> path = new ArrayList<>(path(field.getContainingType()));
    path.addAll(List.of(FIELD, field.getIndex()));
    return Optional.ofNullable(comments.get(path));
  }

  private List<Integer> path(Descriptor message) {
    if (message.getContainingType() == null) {
      return List.of(MESSAGE_TYPE, message.getIndex());
    }
    List<Integer> path = new ArrayList<>(path(message.getContainingType()));
    path.addAll(List.of(NESTED_TYPE, message.getIndex()));
    return path;
  }
}
