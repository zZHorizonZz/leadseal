package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Generated;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.protobuf.column.ColumnTypeOption;
import dev.horizon.leadseal.protobuf.column.FieldKind;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ColumnField {

  private final FieldDescriptor field;
  private final ColumnOptions options;
  private final String name;
  private final FieldKind kind;
  private final ColumnTypeOption type;

  private ColumnField(FieldDescriptor field, ColumnOptions options) {
    this.field = field;
    this.options = options;
    this.name = options.getName().isEmpty() ? field.getName() : options.getName();
    this.kind = FieldKind.of(field);
    this.type = ColumnTypeOption.of(options);
  }

  static Optional<ColumnField> of(FieldDescriptor field) {
    ColumnOptions options = field.getOptions().getExtension(OptionsProto.column);
    return options.getIgnore() ? Optional.empty() : Optional.of(new ColumnField(field, options));
  }

  String name() {
    return name;
  }

  boolean named(String field) {
    return this.field.getName().equals(field);
  }

  String field() {
    return field.getName();
  }

  boolean key() {
    return options.getPrimaryKey();
  }

  Column column(SourceComments comments) {
    List<ColumnAttribute> attributes = new ArrayList<>(type.attributes(kind));
    Type option = type.type(kind);
    if (!kind.holds(option)) {
      throw new InvalidSchemaException("The field " + field.getFullName() + " cannot have the column type " + option
          + ", because its values are " + kind.natural() + ".");
    }
    Type columnType = kind.columnType(option);
    if (!nullable()) {
      attributes.addFirst(NotNull.INSTANCE);
    }
    comments.of(field).ifPresent(comment -> attributes.add(new Comment(comment)));
    return new Column(String.valueOf(field.getNumber()), name, columnType, attributes);
  }

  List<TableAttribute> attributes(TableMessage owner, MessageCatalog catalog) {
    List<TableAttribute> attributes = new ArrayList<>();
    if (options.getUnique() || options.getIndex()) {
      attributes.add(Index.of(owner.name().name(), List.of(name), options.getUnique()));
    }
    if (!options.getCheck().isEmpty()) {
      attributes.add(Check.of(owner.name().name(), options.getCheck()));
    }
    if (options.hasForeignKey()) {
      attributes.add(ForeignKeyOption.of(options.getForeignKey(), field).attribute(owner, catalog));
    }
    return attributes;
  }

  boolean rewrites() {
    return !options.getRewrite().isBlank();
  }

  boolean generates() {
    return !options.getGenerated().isBlank();
  }

  Column generate(CelTable table, Column column) {
    return generates() ? column.with(new Generated(table.parse(options.getGenerated(), Set.of(Row.CURRENT),
        column.type()))) : column;
  }

  Optional<Rewrite> rewrite(CelTable table, Type type) {
    return options.getRewrite().isBlank() ? Optional.empty()
        : Optional.of(new Rewrite(name, table.parse(options.getRewrite(), EnumSet.allOf(Row.class), type)));
  }

  private boolean nullable() {
    if (generates() && options.hasNullable() && !options.getNullable()) {
      throw new InvalidSchemaException("The field " + field.getFullName() + " is generated and does not accept NULL."
          + " A generated column accepts NULL: remove nullable.");
    }
    return generates() || (options.hasNullable() ? options.getNullable()
        : field.isRepeated() || field.hasPresence() && !field.isRequired());
  }
}
