package dev.horizon.leadseal;

import java.math.BigDecimal;

/**
 * A floating-point number.
 *
 * @param size the size of the number
 */
public record FloatType(Size size) implements Type {

  public FloatType {
    if (size == null) {
      throw new SchemaException("A floating-point type has no size.");
    }
  }

  /** The size of a floating-point number. */
  public enum Size {
    /** A 32-bit floating-point number. */
    FLOAT32,
    /** A 64-bit floating-point number. */
    FLOAT64
  }

  @Override
  public String zero() {
    return "0";
  }

  @Override
  public boolean accepts(String value) {
    try {
      new BigDecimal(value);
      return true;
    } catch (NumberFormatException notANumber) {
      return false;
    }
  }

  @Override
  public String example() {
    return "1.5";
  }

  @Override
  public String toString() {
    return size == Size.FLOAT32 ? "32-bit floating-point number" : "64-bit floating-point number";
  }
}
