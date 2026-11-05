package dev.horizon.leadseal.protobuf;

import dev.horizon.aquery.cel.Type.Scalar;
import dev.horizon.aquery.cel.expression.Expression;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.Conditional;
import dev.horizon.leadseal.expression.In;
import dev.horizon.leadseal.expression.IsNull;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Logic;
import dev.horizon.leadseal.expression.Negative;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimestampType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

record CelTranslation(String text) {

  SqlExpression expression(Expression node) {
    if (node.type() == Scalar.DURATION) {
      throw new UnsupportedExpressionException(this, "durations are not supported in an expression yet");
    }
    return switch (node) {
      case Expression.Literal literal -> literal(literal.value());
      case Expression.FieldReference field -> column(field);
      case Expression.Presence presence -> new IsNull(expression(presence.operand())).negate();
      case Expression.Not not -> expression(not.operand()).negate();
      case Expression.Negative negative -> new Negative(expression(negative.operand()));
      case Expression.BinaryOperation binary -> binary(binary);
      case Expression.Conditional conditional -> new Conditional(expression(conditional.condition()),
          expression(conditional.then()), expression(conditional.otherwise()));
      case Expression.Membership membership -> membership(membership);
      case Expression.FunctionCall call -> call(call);
      case Expression.StringMatch _ ->
        throw new UnsupportedExpressionException(this,
            "startsWith(), endsWith() and contains() are not supported in an expression yet");
      default -> throw new UnsupportedExpressionException(this,
          "an expression reads only the fields of its table, not lists, maps, messages,"
              + " parameters or related rows");
    };
  }

  private SqlExpression literal(Object value) {
    return switch (value) {
      case Boolean bool -> new Literal(new BooleanType(), bool.toString());
      case Long number -> new Literal(new IntegerType(IntegerType.Size.INT64), number.toString());
      case Double number -> new Literal(new FloatType(FloatType.Size.FLOAT64),
          BigDecimal.valueOf(number).toPlainString());
      case String string -> new Literal(TextType.unlimited(), string);
      case byte[] bytes -> new Literal(BytesType.unlimited(), HexFormat.of().formatHex(bytes));
      case Instant instant -> new Literal(new TimestampType(), instant.toString());
      default -> throw new UnsupportedExpressionException(this, "the constant " + value + " is not supported in an expression");
    };
  }

  private SqlExpression column(Expression.FieldReference field) {
    String column = field.field().column();
    return switch (field.row()) {
      case Expression.Row.Current _ -> ColumnReference.of(column);
      case Expression.Row.Named(String row) -> new ColumnReference(column, Row.valueOf(row.toUpperCase(Locale.ROOT)));
      case Expression.Row.Related _ ->
        throw new UnsupportedExpressionException(this, "an expression reads only the fields of its table, not related rows");
    };
  }

  private SqlExpression binary(Expression.BinaryOperation binary) {
    SqlExpression left = expression(binary.left());
    SqlExpression right = expression(binary.right());
    return switch (binary.operator()) {
      case AND -> new Logic(Logic.Operator.AND, left, right);
      case OR -> new Logic(Logic.Operator.OR, left, right);
      case EQUALS -> new Comparison(Comparison.Operator.EQUALS, left, right);
      case NOT_EQUALS -> new Comparison(Comparison.Operator.NOT_EQUALS, left, right);
      case LESS -> new Comparison(Comparison.Operator.LESS, left, right);
      case LESS_EQUALS -> new Comparison(Comparison.Operator.LESS_EQUALS, left, right);
      case GREATER -> new Comparison(Comparison.Operator.GREATER, left, right);
      case GREATER_EQUALS -> new Comparison(Comparison.Operator.GREATER_EQUALS, left, right);
      case ADD -> new Arithmetic(binary.type() == Scalar.STRING ? Arithmetic.Operator.CONCAT
          : Arithmetic.Operator.ADD, left, right);
      case SUBTRACT -> new Arithmetic(Arithmetic.Operator.SUBTRACT, left, right);
      case MULTIPLY -> new Arithmetic(Arithmetic.Operator.MULTIPLY, left, right);
      case DIVIDE -> new Arithmetic(binary.type() == Scalar.INT ? Arithmetic.Operator.INTEGER_DIVIDE
          : Arithmetic.Operator.DIVIDE, left, right);
      case MODULO -> new Arithmetic(Arithmetic.Operator.MODULO, left, right);
    };
  }

  private SqlExpression membership(Expression.Membership membership) {
    if (!(membership.collection() instanceof Expression.ListLiteral(List<Expression> elements, _, _))) {
      throw new UnsupportedExpressionException(this, "'in' takes a list of values, as in state in ['OPEN', 'PAID']");
    }
    return new In(expression(membership.element()), elements.stream().map(this::expression).toList());
  }

  private SqlExpression call(Expression.FunctionCall call) {
    if (!(call.function() instanceof CelFunction function)) {
      throw new UnsupportedExpressionException(this,
          "the function " + call.function().name() + "() is not supported in an expression");
    }
    return function.expression(call, this);
  }
}
