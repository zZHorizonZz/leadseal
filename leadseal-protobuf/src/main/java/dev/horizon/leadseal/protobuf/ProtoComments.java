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

/**
 * The leading comments of the messages and fields of a protobuf file, from its source code info. protoc gives source
 * code info to plugins. Descriptors of generated Java classes have none.
 */
final class ProtoComments {

  private static final int MESSAGE_TYPE = 4;
  private static final int NESTED_TYPE = 3;
  private static final int FIELD = 2;

  private final Map<List<Integer>, String> comments = new HashMap<>();

  ProtoComments(FileDescriptor file) {
    for (SourceCodeInfo.Location location : file.toProto().getSourceCodeInfo().getLocationList()) {
      if (location.hasLeadingComments()) {
        String text = location.getLeadingComments().lines().map(String::strip).collect(Collectors.joining("\n"))
            .strip();
        if (!text.isEmpty()) {
          comments.put(List.copyOf(location.getPathList()), text);
        }
      }
    }
  }

  /**
   * Returns the leading comment of a message.
   *
   * @param message the message
   * @return the comment, or an empty optional if the message has none
   */
  Optional<String> of(Descriptor message) {
    return Optional.ofNullable(comments.get(path(message)));
  }

  /**
   * Returns the leading comment of a field.
   *
   * @param field the field
   * @return the comment, or an empty optional if the field has none
   */
  Optional<String> of(FieldDescriptor field) {
    List<Integer> path = new ArrayList<>(path(field.getContainingType()));
    path.addAll(List.of(FIELD, field.getIndex()));
    return Optional.ofNullable(comments.get(path));
  }

  private static List<Integer> path(Descriptor message) {
    if (message.getContainingType() == null) {
      return List.of(MESSAGE_TYPE, message.getIndex());
    }
    List<Integer> path = new ArrayList<>(path(message.getContainingType()));
    path.addAll(List.of(NESTED_TYPE, message.getIndex()));
    return path;
  }
}
