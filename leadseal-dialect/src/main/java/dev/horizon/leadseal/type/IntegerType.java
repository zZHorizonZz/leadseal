package dev.horizon.leadseal.type;

import dev.horizon.leadseal.InvalidSchemaException;

/**
 * An integer of 32 or 64 bits.
 *
 * @param size the size
 */
public record IntegerType(Size size) implements NumberType {

  public IntegerType {
    if (size == null) {
      throw new InvalidSchemaException("An integer type has no size.");
    }
  }

  @Override
  public boolean accepts(String value) {
    try {
      return size.fits(Long.parseLong(value));
    } catch (NumberFormatException _) {
      return false;
    }
  }

  @Override
  public String example() {
    return "42";
  }

  @Override
  public boolean counts() {
    return true;
  }

  @Override
  public String toString() {
    return size.label;
  }

  /** The sizes of an integer. */
  public enum Size {
    /** 32 bits. */
    INT32(-(1L << 31), (1L << 31) - 1, "32-bit integer") {
      @Override
      public <T> T select(T int32, T int64) {
        return int32;
      }
    },
    /** 64 bits. */
    INT64(Long.MIN_VALUE, Long.MAX_VALUE, "64-bit integer") {
      @Override
      public <T> T select(T int32, T int64) {
        return int64;
      }
    };

    private final long min;
    private final long max;
    private final String label;

    Size(long min, long max, String label) {
      this.min = min;
      this.max = max;
      this.label = label;
    }

    /**
     * Tells if a number fits this size.
     *
     * @param number the number
     * @return true if the number fits
     */
    public boolean fits(long number) {
      return number >= min && number <= max;
    }

    /**
     * Gives the value of this size, such as the name of the database type.
     *
     * @param int32 the value of {@link #INT32}
     * @param int64 the value of {@link #INT64}
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
      return this == INT64 || this == other;
    }

    public abstract <T> T select(T int32, T int64);
  }
}
