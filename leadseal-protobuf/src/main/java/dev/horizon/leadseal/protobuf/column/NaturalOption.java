package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.type.Type;
import java.util.List;

record NaturalOption() implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return kind.natural();
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return List.of();
  }
}
