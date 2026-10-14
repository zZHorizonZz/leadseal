package dev.horizon.leadseal;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** A time of day with microsecond precision. Values look like {@code 07:00:00} or {@code 07:00:00.250}. */
public record TimeType() implements Type {

  @Override
  public String zero() {
    return "00:00:00";
  }

  @Override
  public boolean accepts(String value) {
    try {
      LocalTime.parse(value);
      return true;
    } catch (DateTimeParseException notATime) {
      return false;
    }
  }

  @Override
  public String example() {
    return "07:00:00";
  }

  @Override
  public String toString() {
    return "time";
  }
}
