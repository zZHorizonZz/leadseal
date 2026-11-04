package dev.horizon.leadseal.protobuf;

import dev.horizon.aquery.cel.Scope;
import dev.horizon.aquery.cel.Type.Scalar;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.NumberType;
import dev.horizon.leadseal.type.TemporalType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.util.Optional;

record CelScalar(Type type) {

  Optional<Scalar> scalar() {
    return switch (type) {
      case BooleanType _ -> Optional.of(Scalar.BOOL);
      case IntegerType _ -> Optional.of(Scalar.INT);
      case FloatType _,DecimalType _ -> Optional.of(Scalar.DOUBLE);
      case TextType _,UuidType _ -> Optional.of(Scalar.STRING);
      case BytesType _ -> Optional.of(Scalar.BYTES);
      case TimestampType _ -> Optional.of(Scalar.TIMESTAMP);
      case DateType _,TimeType _,JsonType _ -> Optional.empty();
    };
  }

  void declare(Scope.Builder builder, String field, String column, boolean nullable) {
    switch (type) {
      case UuidType _ -> builder.uuid(field, column, nullable);
      case BooleanType _,NumberType _,TextType _,BytesType _,TemporalType _,JsonType _ -> scalar().ifPresent(
          scalar -> {
            if (nullable) {
              builder.nullable(field, scalar, column);
            } else {
              builder.column(field, scalar, column);
            }
          });
    }
  }
}
