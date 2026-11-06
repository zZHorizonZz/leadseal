package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.TableName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class MessageCatalog {

  private final Map<String, TableMessage> tables = new LinkedHashMap<>();

  MessageCatalog(List<FileDescriptor> files) {
    for (FileDescriptor file : files) {
      SourceComments comments = new SourceComments(file);
      file.getMessageTypes().forEach(message -> add(message, comments));
    }
  }

  List<TableMessage> tables() {
    return List.copyOf(tables.values());
  }

  Optional<TableMessage> table(TableName name) {
    return tables.values().stream().filter(table -> table.name().equals(name)).findFirst();
  }

  TableMessage table(String name, String reference) {
    TableMessage message = tables.get(name);
    if (message == null) {
      throw new InvalidSchemaException(reference + " references " + name + ", which is not a table message. Use the"
          + " full message name of a message with the (leadseal.v1.table) option.");
    }
    return message;
  }

  private void add(Descriptor message, SourceComments comments) {
    if (message.getOptions().hasExtension(OptionsProto.table)) {
      tables.put(message.getFullName(), new TableMessage(message, comments));
    }
    message.getNestedTypes().forEach(inner -> add(inner, comments));
  }
}
