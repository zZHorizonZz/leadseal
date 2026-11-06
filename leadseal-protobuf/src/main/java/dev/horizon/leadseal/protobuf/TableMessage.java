package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    Table table = new Table(id, name.name(), columns, attributes);
    if (fields.stream().noneMatch(field -> field.rewrites() || field.generates()) && options.getTriggerList()
        .isEmpty()) {
      return table;
    }
    CelTable expressions = new CelTable(table, fields(), links(table, catalog));
    Table definition = table.withColumns(fields.stream().map(field -> field.generate(expressions, table.column(field
        .name()).orElseThrow())).toList());
    List<Rewrite> rewrites = fields.stream().flatMap(field -> field.rewrite(expressions, table.column(field.name())
        .orElseThrow().type()).stream()).toList();
    List<Trigger> triggers = options.getTriggerList().stream().map(trigger -> new TriggerOption(trigger, message
        .getFullName()).trigger(expressions, catalog)).toList();
    return rewrites.isEmpty() && triggers.isEmpty() ? definition : definition.with(new Triggers(rewrites, triggers));
  }

  Table shape() {
    return new Table(id, name.name(), fields.stream().map(field -> field.column(comments)).toList(),
        List.of(PrimaryKey.of(name.name(), primaryKey())));
  }

  Map<String, String> fields() {
    return fields.stream().collect(Collectors.toMap(ColumnField::field, ColumnField::name, (first, _) -> first,
        LinkedHashMap::new));
  }

  private List<CelTable.Link> links(Table table, MessageCatalog catalog) {
    Map<String, CelTable.Link> links = new LinkedHashMap<>();
    for (ForeignKey key : table.attributes(ForeignKey.class)) {
      if (key.columns().size() != 1) {
        continue;
      }
      String column = key.columns().getFirst();
      fields.stream().filter(field -> field.name().equals(column) && field.field().endsWith("_id")).findFirst()
          .map(field -> field.field().substring(0, field.field().length() - 3))
          .filter(name -> !fields().containsKey(name))
          .ifPresent(name -> catalog.table(key.table()).ifPresent(target -> links.putIfAbsent(name, new CelTable.Link(
              name, column, key.table(), key.referencedColumns().getFirst(), target.shape(), target.fields()))));
    }
    return List.copyOf(links.values());
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
