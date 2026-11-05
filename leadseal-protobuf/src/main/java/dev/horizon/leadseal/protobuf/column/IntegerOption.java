package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.protobuf.IntegerColumn;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.List;

record IntegerOption(IntegerColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new IntegerType(size(kind));
  }

  private IntegerType.Size size(FieldKind kind) {
    return switch (option.getFormat()) {
      case FORMAT_INT32 -> IntegerType.Size.INT32;
      case FORMAT_INT64 -> IntegerType.Size.INT64;
      case FORMAT_UNSPECIFIED, UNRECOGNIZED -> kind.natural() instanceof IntegerType(IntegerType.Size size) ? size
          : IntegerType.Size.INT32;
    };
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    List<ColumnAttribute> all = new ArrayList<>();
    if (option.hasDefaultValue()) {
      all.add(kind.literal(String.valueOf(option.getDefaultValue())));
    }
    if (option.getIdentity()) {
      all.add(Identity.INSTANCE);
    }
    return all;
  }
}
