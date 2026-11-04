package dev.horizon.leadseal.type;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** A time of day. */
public record TimeType() implements TemporalType {

  @Override
  public String zero() {
    return "00:00:00";
  }

  @Override
  public boolean accepts(String value) {
    try {
      LocalTime.parse(value);
      return true;
    } catch (DateTimeParseException _) {
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
