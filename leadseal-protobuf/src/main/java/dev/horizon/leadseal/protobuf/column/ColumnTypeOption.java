package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.protobuf.ColumnOptions;
import dev.horizon.leadseal.type.Type;
import java.util.List;

public sealed interface ColumnTypeOption permits NaturalOption, BooleanOption, IntegerOption, NumberOption,
    DecimalOption, TextOption, BytesOption, UuidOption, TimestampOption, DateOption, TimeOption, JsonOption {

  static ColumnTypeOption of(ColumnOptions options) {
    return switch (options.getTypeCase()) {
      case TYPE_NOT_SET -> new NaturalOption();
      case BOOLEAN -> new BooleanOption(options.getBoolean());
      case INTEGER -> new IntegerOption(options.getInteger());
      case NUMBER -> new NumberOption(options.getNumber());
      case DECIMAL -> new DecimalOption(options.getDecimal());
      case TEXT -> new TextOption(options.getText());
      case BYTES -> new BytesOption(options.getBytes());
      case UUID -> new UuidOption(options.getUuid());
      case TIMESTAMP -> new TimestampOption(options.getTimestamp());
      case DATE -> new DateOption(options.getDate());
      case TIME -> new TimeOption(options.getTime());
      case JSON -> new JsonOption(options.getJson());
    };
  }

  Type type(FieldKind kind);

  List<ColumnAttribute> attributes(FieldKind kind);

  default SchemaException conflict(FieldKind kind) {
    return new InvalidSchemaException("The field " + kind.field() + " has both a default_value and a generated default."
        + " Set one of them.");
  }
}
