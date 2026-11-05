package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.JsonColumn;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.Type;
import java.util.List;

record JsonOption(JsonColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new JsonType();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return option.hasDefaultValue() ? List.of(kind.literal(option.getDefaultValue())) : List.of();
  }
}
