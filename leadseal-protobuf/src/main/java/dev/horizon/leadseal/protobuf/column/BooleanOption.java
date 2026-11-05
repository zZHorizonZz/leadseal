package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.BooleanColumn;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.Type;
import java.util.List;

record BooleanOption(BooleanColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new BooleanType();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return option.hasDefaultValue() ? List.of(kind.literal(String.valueOf(option.getDefaultValue()))) : List.of();
  }
}
