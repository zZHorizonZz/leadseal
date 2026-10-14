package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.TableAttribute;
import java.util.ArrayList;
import java.util.List;

/**
 * A field of a table message and the column it maps to.
 *
 * @param field the field
 * @param messages all table messages, for foreign keys and comments
 */
record ProtoColumn(FieldDescriptor field, ProtoMessages messages) {

  /**
   * Returns the column options of the field.
   *
   * @return the options
   */
  ColumnOptions options() {
    return field.getOptions().getExtension(OptionsProto.column);
  }

  /**
   * Returns whether the field has no column.
   *
   * @return {@code true} if the {@code ignore} option is set
   */
  boolean ignored() {
    return options().getIgnore();
  }

  /**
   * Returns the column name: the {@code name} option, or the field name.
   *
   * @return the column name
   */
  String name() {
    return options().getName().isEmpty() ? field.getName() : options().getName();
  }

  /**
   * Returns the column of the field.
   *
   * @return the column
   * @throws SchemaException if an option is invalid
   */
  Column column() {
    ProtoColumnType type = new ProtoColumnType(field, options());
    List<ColumnAttribute> attributes = new ArrayList<>(type.attributes());
    if (!nullable()) {
      attributes.addFirst(NotNullAttribute.INSTANCE);
    }
    messages.comments(field.getContainingType()).of(field)
        .ifPresent(comment -> attributes.add(new CommentAttribute(comment)));
    return new Column(String.valueOf(field.getNumber()), name(), type.type(), attributes);
  }

  /**
   * Returns the attributes that the field adds to its table: a single-column index, a check and a foreign key.
   *
   * @param table the table name
   * @return the attributes
   */
  List<TableAttribute> tableAttributes(String table) {
    List<TableAttribute> attributes = new ArrayList<>();
    ColumnOptions options = options();
    if (options.getUnique() || options.getIndex()) {
      attributes.add(IndexAttribute.of(table, List.of(name()), options.getUnique()));
    }
    if (!options.getCheck().isEmpty()) {
      attributes.add(CheckAttribute.of(table, options.getCheck()));
    }
    if (options.hasForeignKey()) {
      attributes.add(foreignKey(table, options.getForeignKey()));
    }
    return attributes;
  }

  private boolean nullable() {
    return options().hasNullable() ? options().getNullable()
        : field.isRepeated() || field.hasPresence() && !field.isRequired();
  }

  private ForeignKeyAttribute foreignKey(String table, ForeignKeyOptions options) {
    ProtoTable target = new ProtoTable(messages.table(options.getMessage(), field.getFullName()), messages);
    String referenced;
    if (options.getField().isEmpty()) {
      List<String> keys = target.primaryKey();
      if (keys.size() != 1) {
        throw new SchemaException("The foreign key of " + field.getFullName() + " needs a field, because "
            + target.message().getFullName() + " has " + keys.size() + " primary key fields.");
      }
      referenced = keys.getFirst();
    } else {
      referenced = target.columnName(options.getField());
    }
    ForeignKeyAttribute key = ForeignKeyAttribute.of(table, List.of(name()), messages.name(target.message()),
        List.of(referenced), new ProtoAction(options.getOnDelete()).value())
        .withOnUpdate(new ProtoAction(options.getOnUpdate()).value());
    return options.getName().isEmpty() ? key : key.withName(options.getName());
  }
}
