package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.TextColumn;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.List;

record TextOption(TextColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return option.getLength() == 0 ? TextType.unlimited() : TextType.of(option.getLength());
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    List<ColumnAttribute> all = new ArrayList<>();
    if (option.hasDefaultValue()) {
      all.add(kind.text(option.getDefaultValue()));
    }
    option.getCollationMap().forEach((dialect, collation) -> all.add(new Collation(dialect, collation)));
    return all;
  }
}
