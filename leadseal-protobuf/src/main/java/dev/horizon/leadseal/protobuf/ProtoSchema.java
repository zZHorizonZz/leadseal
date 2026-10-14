package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads the table messages of protobuf files into a schema.
 *
 * <p>
 * A message is a table if it has the {@code (leadseal.v1.table)} option. Each field is a column, unless its
 * {@code (leadseal.v1.column)} option has {@code ignore} set. The table id is the full message name without the
 * version parts of its package, such as {@code v1}, and the column id is the field number. Renaming a message or a
 * field, or moving a message to a new version of its package, renames its table or column and keeps the rows. The
 * leading comments of messages and fields are the comments of their tables and columns. A table is in the namespace of
 * its {@code schema} option, or in the default namespace.
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
    ProtoMessages messages = new ProtoMessages(files);
    Map<String, List<Table>> namespaces = new LinkedHashMap<>();
    for (Descriptor message : messages.tables()) {
      TableName name = messages.name(message);
      namespaces.computeIfAbsent(Objects.requireNonNullElse(name.namespace(), ""), ignored -> new ArrayList<>())
          .add(new ProtoTable(message, messages).table());
    }
    List<Namespace> result = new ArrayList<>();
    namespaces.forEach((name, tables) -> result.add(new Namespace(name.isEmpty() ? null : name, tables)));
    return new Schema(result);
  }
}
