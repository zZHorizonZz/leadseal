package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.TableName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** The table messages of protobuf files, with their table names and the comments of their files. */
final class ProtoMessages {

  private static final Pattern VERSION = Pattern.compile("v\\d+((alpha|beta)\\d*)?");

  private final Map<String, Descriptor> tables = new LinkedHashMap<>();
  private final Map<FileDescriptor, ProtoComments> comments = new HashMap<>();

  ProtoMessages(List<FileDescriptor> files) {
    for (FileDescriptor file : files) {
      for (Descriptor message : file.getMessageTypes()) {
        collect(message);
      }
    }
  }

  /**
   * Returns the table messages.
   *
   * @return the messages, in the order of their files
   */
  List<Descriptor> tables() {
    return List.copyOf(tables.values());
  }

  /**
   * Returns the table message with a full name.
   *
   * @param name the full message name, for example {@code shop.v1.Order}
   * @param owner the element that refers to the message, for the error message
   * @return the message
   * @throws SchemaException if no table message has that name
   */
  Descriptor table(String name, String owner) {
    Descriptor message = tables.get(name);
    if (message == null) {
      throw new SchemaException("The foreign key of " + owner + " references " + name + ", which is not a table"
          + " message. Use the full message name of a message with the (leadseal.v1.table) option.");
    }
    return message;
  }

  /**
   * Returns the table name of a message: its {@code name} option, or the message name in snake_case, in the
   * namespace of its {@code schema} option.
   *
   * @param message the table message
   * @return the table name
   */
  TableName name(Descriptor message) {
    TableOptions options = message.getOptions().getExtension(OptionsProto.table);
    String name = options.getName().isEmpty() ? snakeCase(message.getName()) : options.getName();
    return new TableName(options.getSchema().isEmpty() ? null : options.getSchema(), name);
  }

  /**
   * Returns the table id of a message: its {@code id} option, or the full message name without the version parts of
   * its package.
   *
   * @param message the table message
   * @return the id, for example {@code shop.Order} for {@code shop.v2.Order}
   */
  String id(Descriptor message) {
    TableOptions options = message.getOptions().getExtension(OptionsProto.table);
    if (!options.getId().isEmpty()) {
      return options.getId();
    }
    String pkg = message.getFile().getPackage();
    String local = pkg.isEmpty() ? message.getFullName() : message.getFullName().substring(pkg.length() + 1);
    String unversioned = Arrays.stream(pkg.split("\\.")).filter(part -> !part.isEmpty())
        .filter(part -> !VERSION.matcher(part).matches()).collect(Collectors.joining("."));
    return unversioned.isEmpty() ? local : unversioned + "." + local;
  }

  /**
   * Returns the comments of the file of a message.
   *
   * @param message the message
   * @return the comments
   */
  ProtoComments comments(Descriptor message) {
    return comments.computeIfAbsent(message.getFile(), ProtoComments::new);
  }

  private void collect(Descriptor message) {
    if (message.getOptions().hasExtension(OptionsProto.table)) {
      tables.put(message.getFullName(), message);
    }
    for (Descriptor nested : message.getNestedTypes()) {
      collect(nested);
    }
  }

  private static String snakeCase(String name) {
    StringBuilder snake = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean boundary = i > 0 && Character.isUpperCase(c) && (Character.isLowerCase(name.charAt(i - 1))
          || i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)));
      snake.append(boundary ? "_" : "").append(c);
    }
    return snake.toString().toLowerCase(Locale.ROOT);
  }
}
