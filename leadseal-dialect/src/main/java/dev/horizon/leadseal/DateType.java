package dev.horizon.leadseal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** A date without a time. Values look like {@code 2026-10-14}. */
public record DateType() implements Type {

  @Override
  public String zero() {
    return "1970-01-01";
  }

  @Override
  public boolean accepts(String value) {
    try {
      LocalDate.parse(value);
      return true;
    } catch (DateTimeParseException notADate) {
      return false;
    }
  }

  @Override
  public String example() {
    return "2026-10-14";
  }

  @Override
  public String toString() {
    return "date";
  }
}
