package dev.horizon.leadseal.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.horizon.leadseal.InvalidSchemaException;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * The length of a text or of bytes: a limit, or no limit.
 */
public sealed interface Length {

  /** No limit. */
  Length UNLIMITED = new Unlimited();

  /**
   * Gives a length from its form in JSON.
   *
   * @param value the limit, or {@code null} for no limit
   * @return the length
   */
  @JsonCreator
  static Length of(Long value) {
    return value == null ? UNLIMITED : new Limit(Math.toIntExact(value));
  }

  /**
   * Gives a value for the limit of this length or for no limit.
   *
   * @param limit the value for a limit of a number of characters or bytes
   * @param unlimited the value for no limit
   * @param <T> the type of the value
   * @return the value
   */
  <T> T map(IntFunction<T> limit, Supplier<T> unlimited);

  /**
   * Tells if a value of a size fits this length.
   *
   * @param size the number of characters or bytes of the value
   * @return true if the value fits
   */
  boolean fits(int size);

  /**
   * Gives the form of this length in JSON.
   *
   * @return the limit, or {@code null} for no limit, which JSON leaves out
   */
  @JsonValue
  Long json();

  /**
   * A limit.
   *
   * @param value the largest number of characters or bytes
   */
  record Limit(int value) implements Length {

    public Limit {
      if (value <= 0) {
        throw new InvalidSchemaException("A length must be greater than 0, but is " + value + ".");
      }
    }

    @Override
    public <T> T map(IntFunction<T> limit, Supplier<T> unlimited) {
      return limit.apply(value);
    }

    @Override
    public boolean fits(int size) {
      return size <= value;
    }

    @Override
    public Long json() {
      return (long) value;
    }
  }

  /** No limit. */
  record Unlimited() implements Length {

    @Override
    public <T> T map(IntFunction<T> limit, Supplier<T> unlimited) {
      return unlimited.get();
    }

    @Override
    public boolean fits(int size) {
      return true;
    }

    @Override
    public Long json() {
      return null;
    }
  }
}
