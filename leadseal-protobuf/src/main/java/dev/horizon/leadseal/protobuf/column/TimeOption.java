package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.TimeColumn;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.Type;
import java.util.List;

record TimeOption(TimeColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new TimeType();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return option.hasDefaultValue() ? List.of(kind.literal(option.getDefaultValue())) : List.of();
  }
}
