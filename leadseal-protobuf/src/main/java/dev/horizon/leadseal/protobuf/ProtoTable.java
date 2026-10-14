package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.attribute.TableAttribute;
import java.util.ArrayList;
import java.util.List;

/**
 * A table message and the table it maps to.
 *
 * @param message the table message
 * @param messages all table messages, for foreign keys
 */
record ProtoTable(Descriptor message, ProtoMessages messages) {

  /**
   * Returns the table of the message.
   *
   * @return the table
   * @throws SchemaException if an option is invalid or the message has no primary key field
   */
  Table table() {
    String name = messages.name(message).name();
    TableOptions options = message.getOptions().getExtension(OptionsProto.table);
    List<Column> columns = new ArrayList<>();
    List<String> primaryKey = new ArrayList<>();
    List<TableAttribute> attributes = new ArrayList<>();
    for (FieldDescriptor field : message.getFields()) {
      ProtoColumn column = new ProtoColumn(field, messages);
      if (column.ignored()) {
        continue;
      }
      columns.add(column.column());
      if (column.options().getPrimaryKey()) {
        primaryKey.add(column.name());
      }
      attributes.addAll(column.tableAttributes(name));
    }
    if (primaryKey.isEmpty()) {
      throw new SchemaException("The table message " + message.getFullName() + " has no primary key. Set"
          + " (leadseal.v1.column).primary_key on its key fields.");
    }
    attributes.addFirst(PrimaryKeyAttribute.of(name, primaryKey));
    for (IndexOptions index : options.getIndexList()) {
      attributes.add(index(name, index));
    }
    for (CheckOptions check : options.getCheckList()) {
      attributes.add(check.getName().isEmpty() ? CheckAttribute.of(name, check.getExpression())
          : new CheckAttribute(check.getName(), check.getExpression()));
    }
    for (TableForeignKeyOptions key : options.getForeignKeyList()) {
      attributes.add(foreignKey(name, key));
    }
    messages.comments(message).of(message).ifPresent(comment -> attributes.add(new CommentAttribute(comment)));
    return new Table(messages.id(message), name, columns, attributes);
  }

  /**
   * Returns the column name of a field of this message.
   *
   * @param field the field name
   * @return the column name
   * @throws SchemaException if the message has no field with that name
   */
  String columnName(String field) {
    FieldDescriptor descriptor = message.findFieldByName(field);
    if (descriptor == null) {
      throw new SchemaException("The message " + message.getFullName() + " has no field '" + field + "'.");
    }
    return new ProtoColumn(descriptor, messages).name();
  }

  /**
   * Returns the column names of the primary key fields of this message.
   *
   * @return the column names, in field order
   */
  List<String> primaryKey() {
    return message.getFields().stream().map(field -> new ProtoColumn(field, messages))
        .filter(column -> column.options().getPrimaryKey()).map(ProtoColumn::name).toList();
  }

  private IndexAttribute index(String table, IndexOptions index) {
    List<String> columns = index.getFieldsList().stream().map(this::columnName).toList();
    IndexAttribute definition = index.getName().isEmpty() ? IndexAttribute.of(table, columns, index.getUnique())
        : new IndexAttribute(index.getName(), columns, index.getUnique(), List.of(), null);
    return definition.withWhere(index.getWhere())
        .withDescending(index.getDescendingList().stream().map(this::columnName).toList());
  }

  private ForeignKeyAttribute foreignKey(String table, TableForeignKeyOptions options) {
    ProtoTable target = new ProtoTable(messages.table(options.getMessage(), message.getFullName()), messages);
    List<String> columns = options.getFieldsList().stream().map(this::columnName).toList();
    List<String> referenced = options.getReferencesList().isEmpty() ? target.primaryKey()
        : options.getReferencesList().stream().map(target::columnName).toList();
    ForeignKeyAttribute key = ForeignKeyAttribute.of(table, columns, messages.name(target.message()), referenced,
        new ProtoAction(options.getOnDelete()).value()).withOnUpdate(new ProtoAction(options.getOnUpdate()).value());
    return options.getName().isEmpty() ? key : key.withName(options.getName());
  }
}
