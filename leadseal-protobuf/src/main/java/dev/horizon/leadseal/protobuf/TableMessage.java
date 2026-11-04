package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.TableAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TableMessage {

  private final Descriptor message;
  private final TableOptions options;
  private final SourceComments comments;
  private final TableName name;
  private final String id;
  private final List<ColumnField> fields;

  TableMessage(Descriptor message, SourceComments comments) {
    this.message = message;
    this.options = message.getOptions().getExtension(OptionsProto.table);
    this.comments = comments;
    MessageName names = new MessageName(message);
    this.name = new TableName(options.getSchema().isEmpty() ? Namespace.Name.DEFAULT
        : Namespace.Name.of(options.getSchema()), options.getName().isEmpty() ? names.table() : options.getName());
    this.id = options.getId().isEmpty() ? names.id() : options.getId();
    this.fields = message.getFields().stream().map(ColumnField::of).flatMap(Optional::stream).toList();
  }

  TableName name() {
    return name;
  }

  Table table(MessageCatalog catalog) {
    List<String> key = primaryKey();
    List<Column> columns = new ArrayList<>();
    List<TableAttribute> attributes = new ArrayList<>();
    for (ColumnField field : fields) {
      columns.add(field.column(comments));
      attributes.addAll(field.attributes(this, catalog));
    }
    if (key.isEmpty()) {
      throw new InvalidSchemaException("The table message " + message.getFullName() + " has no primary key. Set"
          + " (leadseal.v1.column).primary_key on its key fields.");
    }
    attributes.addFirst(PrimaryKey.of(name.name(), key));
    options.getIndexList().forEach(index -> attributes.add(index(index)));
    options.getCheckList().forEach(check -> attributes.add(check.getName().isEmpty() ? Check.of(name.name(),
        check.getExpression()) : new Check(check.getName(), check.getExpression())));
    options.getForeignKeyList().forEach(foreignKey -> attributes.add(ForeignKeyOption.of(foreignKey, message.getFullName())
        .attribute(this, catalog)));
    comments.of(message).ifPresent(comment -> attributes.add(new Comment(comment)));
    return new Table(id, name.name(), columns, attributes);
  }

  String column(String field) {
    return fields.stream().filter(column -> column.named(field)).findFirst().map(ColumnField::name)
        .orElseThrow(() -> new InvalidSchemaException("The message " + message.getFullName() + " has no field '" + field
            + "'."));
  }

  List<String> primaryKey() {
    return fields.stream().filter(ColumnField::key).map(ColumnField::name).toList();
  }

  private Index index(IndexOptions index) {
    List<String> columns = index.getFieldsList().stream().map(this::column).toList();
    Index definition = index.getName().isEmpty() ? Index.of(name.name(), columns, index.getUnique())
        : new Index(index.getName(), columns, index.getUnique(), List.of(), Index.Rows.ALL);
    return definition.withWhere(index.getWhere())
        .withDescending(index.getDescendingList().stream().map(this::column).toList());
  }
}
