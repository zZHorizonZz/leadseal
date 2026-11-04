package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefault;
import dev.horizon.leadseal.protobuf.UuidColumn;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.util.List;

record UuidOption(UuidColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new UuidType();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    if (option.hasDefaultValue() && option.getDefaultRandom()) {
      throw conflict(kind);
    }
    if (option.hasDefaultValue()) {
      return List.of(kind.literal(option.getDefaultValue()));
    }
    return option.getDefaultRandom() ? List.of(new RandomUuidDefault()) : List.of();
  }
}
