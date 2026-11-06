package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.attribute.ForeignKey;
import java.util.List;

record ForeignKeyOption(String origin, String name, List<String> fields, String message, List<String> references,
    Action onDelete, Action onUpdate) {

  static ForeignKeyOption of(ForeignKeyOptions options, FieldDescriptor field) {
    return new ForeignKeyOption(field.getFullName(), options.getName(), List.of(field.getName()),
        options.getMessage(), options.getField().isEmpty() ? List.of() : List.of(options.getField()),
        options.getOnDelete(), options.getOnUpdate());
  }

  static ForeignKeyOption of(TableForeignKeyOptions options, String message) {
    return new ForeignKeyOption(message, options.getName(), options.getFieldsList(), options.getMessage(),
        options.getReferencesList(), options.getOnDelete(), options.getOnUpdate());
  }

  ForeignKey attribute(TableMessage owner, MessageCatalog catalog) {
    TableMessage target = catalog.table(message, "The foreign key of " + origin);
    List<String> targetColumns = references.isEmpty() ? target.primaryKey()
        : references.stream().map(target::column).toList();
    if (references.isEmpty() && targetColumns.size() != fields.size()) {
      throw new InvalidSchemaException("The foreign key of " + origin + " needs a field, because " + message + " has "
          + targetColumns.size() + " primary key fields.");
    }
    ForeignKey key = ForeignKey.of(owner.name().name(), fields.stream().map(owner::column).toList(), target.name(),
        targetColumns, new ActionOption(onDelete, origin).value()).withOnUpdate(new ActionOption(onUpdate, origin).value());
    return name.isEmpty() ? key : key.withName(name);
  }
}
