package dev.horizon.leadseal;

/**
 * An integer.
 *
 * @param size the size of the integer
 */
public record IntegerType(Size size) implements Type {

  public IntegerType {
    if (size == null) {
      throw new SchemaException("An integer type has no size.");
    }
  }

  /** The size of an integer, with its smallest and largest value. */
  public enum Size {
    /** A 32-bit integer. */
    INT32(Integer.MIN_VALUE, Integer.MAX_VALUE),
    /** A 64-bit integer. */
    INT64(Long.MIN_VALUE, Long.MAX_VALUE);

    private final long min;
    private final long max;

    Size(long min, long max) {
      this.min = min;
      this.max = max;
    }

    /**
     * Returns whether a number fits in this size.
     *
     * @param number the number
     * @return {@code true} if the number is between the smallest and the largest value
     */
    public boolean fits(long number) {
      return number >= min && number <= max;
    }
  }

  @Override
  public String zero() {
    return "0";
  }

  @Override
  public boolean accepts(String value) {
    try {
      return size.fits(Long.parseLong(value));
    } catch (NumberFormatException notAnInteger) {
      return false;
    }
  }

  @Override
  public String example() {
    return "42";
  }

  @Override
  public String toString() {
    return size == Size.INT32 ? "32-bit integer" : "64-bit integer";
  }
}
