package dev.horizon.leadseal.type;

/** The type of a number: an integer, a floating-point number or a decimal. The zero of a number is {@code 0}. */
public sealed interface NumberType extends Type permits IntegerType, FloatType, DecimalType {

  @Override
  default String zero() {
    return "0";
  }
}
