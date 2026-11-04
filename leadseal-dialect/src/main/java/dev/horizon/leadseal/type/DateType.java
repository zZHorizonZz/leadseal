package dev.horizon.leadseal.type;

import dev.horizon.leadseal.Dialect;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** A date. */
public record DateType() implements TemporalType {

  @Override
  public String zero() {
    return "1970-01-01";
  }

  @Override
  public boolean accepts(String value) {
    try {
      LocalDate.parse(value);
      return true;
    } catch (DateTimeParseException _) {
      return false;
    }
  }

  @Override
  public String example() {
    return "2026-10-14";
  }

  @Override
  public String current(Dialect dialect) {
    return dialect.currentDate();
  }

  @Override
  public boolean dates() {
    return true;
  }

  @Override
  public String toString() {
    return "date";
  }
}
