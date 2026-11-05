package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.DecimalColumn;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.Type;
import java.util.List;

record DecimalOption(DecimalColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new DecimalType(option.getPrecision(), option.getScale());
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return option.hasDefaultValue() ? List.of(kind.literal(option.getDefaultValue())) : List.of();
  }
}
