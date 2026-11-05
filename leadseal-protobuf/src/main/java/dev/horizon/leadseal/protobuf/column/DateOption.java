package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.NowDefault;
import dev.horizon.leadseal.protobuf.DateColumn;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.Type;
import java.util.List;

record DateOption(DateColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new DateType();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    if (option.hasDefaultValue() && option.getDefaultToday()) {
      throw new DefaultConflictException(kind);
    }
    if (option.hasDefaultValue()) {
      return List.of(kind.literal(option.getDefaultValue()));
    }
    return option.getDefaultToday() ? List.of(new NowDefault()) : List.of();
  }
}
