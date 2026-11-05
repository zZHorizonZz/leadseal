package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.BytesColumn;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.Type;
import java.util.HexFormat;
import java.util.List;

record BytesOption(BytesColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return option.getLength() == 0 ? BytesType.unlimited() : BytesType.of(option.getLength());
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return option.hasDefaultValue() ? List.of(kind.literal(HexFormat.of().formatHex(option.getDefaultValue()
        .toByteArray()))) : List.of();
  }
}
