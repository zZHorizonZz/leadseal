package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.NowDefault;
import dev.horizon.leadseal.protobuf.TimestampColumn;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import java.time.Instant;
import java.util.List;

record TimestampOption(TimestampColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new TimestampType();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    if (option.hasDefaultValue() && option.getDefaultNow()) {
      throw conflict(kind);
    }
    if (option.hasDefaultValue()) {
      return List.of(kind.literal(Instant.ofEpochSecond(option.getDefaultValue().getSeconds(),
          option.getDefaultValue().getNanos()).toString()));
    }
    return option.getDefaultNow() ? List.of(new NowDefault()) : List.of();
  }
}
