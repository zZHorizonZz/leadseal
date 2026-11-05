package dev.horizon.leadseal.protobuf.column;

import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;

record ValueField(String field, Type natural) implements FieldKind {

  @Override
  public boolean holds(Type type) {
    return switch (type) {
      case TextType _ -> natural instanceof TextType;
      case IntegerType(IntegerType.Size size) -> natural instanceof IntegerType(IntegerType.Size own)
          && size.holds(own);
      case FloatType(FloatType.Size size) -> natural instanceof FloatType(FloatType.Size own) && size.holds(own);
      case UuidType _ -> natural instanceof TextType || natural instanceof BytesType;
      case JsonType _,DecimalType _,DateType _,TimeType _,TimestampType _ -> natural.getClass() == type.getClass()
          || natural instanceof TextType;
      case BooleanType _,BytesType _ -> natural.getClass() == type.getClass();
      case EnumType _ -> false;
    };
  }
}
