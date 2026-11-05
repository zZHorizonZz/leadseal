package dev.horizon.leadseal.protobuf;

import dev.horizon.aquery.cel.Scope;
import dev.horizon.aquery.cel.Type;
import dev.horizon.aquery.cel.expression.Expression;
import dev.horizon.aquery.cel.expression.Function;
import dev.horizon.aquery.cel.expression.Overload;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.SqlExpression;
import java.util.List;
import java.util.stream.Stream;

sealed interface CelFunction extends Function permits CelFunction.CallFunction, CelFunction.ChangeFunction {

  List<Type> VALUES = List.of(Type.Scalar.BOOL, Type.Scalar.INT, Type.Scalar.DOUBLE, Type.Scalar.STRING,
      Type.Scalar.BYTES, Type.Scalar.TIMESTAMP);

  SqlExpression expression(Expression.FunctionCall call, CelTranslation translation);

  static List<CelFunction> all() {
    return Stream.concat(Stream.of(Call.Function.values()).flatMap(function -> of(function).stream()),
        VALUES.stream().map(ChangeFunction::new)).toList();
  }

  private static List<CelFunction> of(Call.Function function) {
    return switch (function) {
      case NOW -> List.of(new CallFunction(function, List.of(), Type.Scalar.TIMESTAMP));
      case UUID -> List.of(new CallFunction(function, List.of(), Type.Scalar.STRING));
      case LOWER, UPPER, TRIM -> List.of(new CallFunction(function, List.of(Type.Scalar.STRING), Type.Scalar.STRING));
      case LENGTH -> List.of(new CallFunction(function, List.of(Type.Scalar.STRING), Type.Scalar.INT));
      case ABS -> List.of(new CallFunction(function, List.of(Type.Scalar.INT), Type.Scalar.INT),
          new CallFunction(function, List.of(Type.Scalar.DOUBLE), Type.Scalar.DOUBLE));
      case COALESCE -> VALUES.stream().<CelFunction> map(value -> new CallFunction(function, List.of(value, value), value))
          .toList();
    };
  }

  record CallFunction(Call.Function function, Overload overload) implements CelFunction {

    CallFunction(Call.Function function, List<Type> parameters, Type result) {
      this(function, new Overload.Global(parameters, result));
    }

    @Override
    public String name() {
      return function.describe();
    }

    @Override
    public SqlExpression expression(Expression.FunctionCall call, CelTranslation translation) {
      return new Call(function, call.arguments().stream().map(translation::expression).toList());
    }
  }

  record ChangeFunction(Overload overload) implements CelFunction {

    ChangeFunction(Type value) {
      this(new Overload.Global(List.of(value), Type.Scalar.BOOL));
    }

    @Override
    public String name() {
      return "changed";
    }

    @Override
    public SqlExpression expression(Expression.FunctionCall call, CelTranslation translation) {
      if (call.arguments()
          .getFirst() instanceof Expression.FieldReference(Scope.Field field, Expression.Row.Current _, int _)) {
        return new ColumnChange(field.column());
      }
      throw new UnsupportedExpressionException(translation, "changed() takes a field of the row, as in changed(state)");
    }
  }
}
