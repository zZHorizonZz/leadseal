package dev.horizon.leadseal;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/** A point in time with microsecond precision. Values are ISO 8601 instants, like {@code 2026-10-14T07:00:00Z}. */
public record TimestampType() implements Type {

  @Override
  public String zero() {
    return "1970-01-01T00:00:00Z";
  }

  @Override
  public boolean accepts(String value) {
    try {
      Instant.parse(value);
      return true;
    } catch (DateTimeParseException notAnInstant) {
      return false;
    }
  }

  @Override
  public String example() {
    return "2026-10-14T07:00:00Z";
  }

  @Override
  public String toString() {
    return "timestamp";
  }
}
