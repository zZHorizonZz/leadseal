package dev.horizon.leadseal.type;

import dev.horizon.leadseal.Dialect;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** A point in time, in UTC. */
public record TimestampType() implements TemporalType {

  @Override
  public String zero() {
    return "1970-01-01T00:00:00Z";
  }

  @Override
  public boolean accepts(String value) {
    try {
      Instant.parse(value);
      return true;
    } catch (DateTimeParseException _) {
      return false;
    }
  }

  @Override
  public String example() {
    return "2026-10-14T07:00:00Z";
  }

  @Override
  public String current(Dialect dialect) {
    return dialect.currentTimestamp();
  }

  @Override
  public boolean dates() {
    return true;
  }

  @Override
  public String toString() {
    return "timestamp";
  }
}
