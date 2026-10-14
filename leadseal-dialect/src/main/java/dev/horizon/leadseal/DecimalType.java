package dev.horizon.leadseal;

import java.math.BigDecimal;

/**
 * An exact decimal number.
 *
 * @param precision the number of digits
 * @param scale the number of digits after the decimal point
 */
public record DecimalType(int precision, int scale) implements Type {

  public DecimalType {
    if (precision <= 0) {
      throw new SchemaException("The precision of a decimal type must be greater than 0, but is " + precision + ".");
    }
    if (scale < 0 || scale > precision) {
      throw new SchemaException("The scale of a decimal type must be between 0 and its precision " + precision
          + ", but is " + scale + ".");
    }
  }

  @Override
  public String zero() {
    return "0";
  }

  @Override
  public boolean accepts(String value) {
    try {
      BigDecimal number = new BigDecimal(value);
      return number.scale() <= scale && number.precision() - number.scale() <= precision - scale
          && !value.contains("e") && !value.contains("E");
    } catch (NumberFormatException notANumber) {
      return false;
    }
  }

  @Override
  public String example() {
    return "12.50";
  }

  @Override
  public String toString() {
    return "decimal(" + precision + ", " + scale + ")";
  }
}
