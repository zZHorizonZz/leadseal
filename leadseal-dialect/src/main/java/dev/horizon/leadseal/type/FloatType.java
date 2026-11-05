package dev.horizon.leadseal.type;

import dev.horizon.leadseal.InvalidSchemaException;

import java.math.BigDecimal;

/**
 * A floating-point number of 32 or 64 bits.
 *
 * @param size the size
 */
public record FloatType(Size size) implements NumberType {

  public FloatType {
    if (size == null) {
      throw new InvalidSchemaException("A floating-point type has no size.");
    }
  }

  @Override
  public boolean accepts(String value) {
    try {
      new BigDecimal(value);
      return true;
    } catch (NumberFormatException _) {
      return false;
    }
  }

  @Override
  public String example() {
    return "1.5";
  }

  @Override
  public String toString() {
    return size.label;
  }

  /** The sizes of a floating-point number. */
  public enum Size {
    /** 32 bits. */
    FLOAT32("32-bit floating-point number") {
      @Override
      public <T> T select(T float32, T float64) {
        return float32;
      }
    },
    /** 64 bits. */
    FLOAT64("64-bit floating-point number") {
      @Override
      public <T> T select(T float32, T float64) {
        return float64;
      }
    };

    private final String label;

    Size(String label) {
      this.label = label;
    }

    /**
     * Gives the value of this size, such as the name of the database type.
     *
     * @param float32 the value of {@link #FLOAT32}
     * @param float64 the value of {@link #FLOAT64}
     * @param <T> the type of the value
     * @return the value
     */
    /**
     * Tells if a column of this size holds the values of another size.
     *
     * @param other the size of the values
     * @return true if this size is the other size, or the larger size
     */
    public boolean holds(Size other) {
      return this == FLOAT64 || this == other;
    }

    public abstract <T> T select(T float32, T float64);
  }

  @Override
  public boolean isAssignableFrom(Type origin) {
    return origin instanceof FloatType(Size other) && size.holds(other);
  }
}
