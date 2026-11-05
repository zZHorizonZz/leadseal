package dev.horizon.leadseal.protobuf.column;

import com.google.protobuf.Descriptors.EnumDescriptor;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record EnumField(String field, EnumDescriptor values) implements FieldKind {

  @Override
  public Type natural() {
    return TextType.unlimited();
  }

  @Override
  public boolean holds(Type type) {
    return type instanceof TextType || type instanceof IntegerType;
  }

  @Override
  public LiteralDefault text(String value) {
    if (values.findValueByName(value) == null) {
      throw new UnknownEnumValueException(field, values, value, "name");
    }
    return new LiteralDefault(value);
  }

  @Override
  public LiteralDefault literal(String value) {
    try {
      if (values.findValueByNumber(Integer.parseInt(value)) != null) {
        return new LiteralDefault(value);
      }
    } catch (NumberFormatException _) {
      throw new UnknownEnumValueException(field, values, value, "number");
    }
    throw new UnknownEnumValueException(field, values, value, "number");
  }

  @Override
  public Type columnType(Type type) {
    Map<Integer, EnumType.Value> canonical = new LinkedHashMap<>();
    values.getValues().forEach(value -> canonical.putIfAbsent(value.getNumber(), new EnumType.Value(value.getNumber(),
        value.getName())));
    String file = values.getFile().getPackage();
    String path = file.isEmpty() ? values.getFullName() : values.getFullName().substring(file.length() + 1);
    String name = Arrays.stream(path.split("\\.")).map(part -> new SnakeCase(part).value())
        .collect(Collectors.joining("_"));
    return new EnumType(name, List.copyOf(canonical.values()),
        type instanceof IntegerType ? EnumType.Storage.NUMBER : EnumType.Storage.NAME);
  }
}
