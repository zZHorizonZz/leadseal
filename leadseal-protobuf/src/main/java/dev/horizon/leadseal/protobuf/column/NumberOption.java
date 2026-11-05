package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.NumberColumn;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.Type;
import java.math.BigDecimal;
import java.util.List;

record NumberOption(NumberColumn option) implements ColumnTypeOption {

  @Override
  public Type type(FieldKind kind) {
    return new FloatType(size(kind));
  }

  private FloatType.Size size(FieldKind kind) {
    return switch (option.getFormat()) {
      case FORMAT_FLOAT -> FloatType.Size.FLOAT32;
      case FORMAT_DOUBLE -> FloatType.Size.FLOAT64;
      case FORMAT_UNSPECIFIED, UNRECOGNIZED -> kind.natural() instanceof FloatType(FloatType.Size size) ? size
          : FloatType.Size.FLOAT64;
    };
  }

  @Override
  public List<ColumnAttribute> attributes(FieldKind kind) {
    return option.hasDefaultValue() ? List.of(kind.literal(BigDecimal.valueOf(option.getDefaultValue())
        .toPlainString())) : List.of();
  }
}
