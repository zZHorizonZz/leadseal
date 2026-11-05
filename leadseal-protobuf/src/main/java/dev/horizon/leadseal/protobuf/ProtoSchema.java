package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the schema of the table messages of protobuf files: the messages with the {@code (leadseal.v1.table)} option,
 * and their fields with the {@code (leadseal.v1.column)} option.
 */
public final class ProtoSchema {

  /**
   * Reads the table messages of files, including their nested messages.
   *
   * @param files the files
   * @return the schema
   * @throws SchemaException if an option is invalid, or the tables do not form a valid schema
   */
  public Schema read(List<FileDescriptor> files) {
    MessageCatalog catalog = new MessageCatalog(files);
    Map<Namespace.Name, List<Table>> namespaces = new LinkedHashMap<>();
    for (TableMessage message : catalog.tables()) {
      namespaces.computeIfAbsent(message.name().namespace(), _ -> new ArrayList<>()).add(message.table(catalog));
    }
    List<Namespace> all = new ArrayList<>();
    namespaces.forEach((name, tables) -> all.add(new Namespace(name, tables)));
    return new Schema(all);
  }
}
