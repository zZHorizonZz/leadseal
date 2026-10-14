package dev.horizon.leadseal;

import dev.horizon.leadseal.impl.Json;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Checks values in the canonical text form of a column type. */
final class Values {

  private static final Pattern INTEGER = Pattern.compile("-?\\d+");
  private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?([eE][-+]?\\d+)?");
  private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
  private static final Pattern HEX = Pattern.compile("([0-9a-fA-F]{2})*");
  private static final Pattern UUID = Pattern.compile(
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  private Values() {
  }

  static boolean valid(ColumnType type, String value) {
    return switch (type) {
      case BOOLEAN -> value.equals("true") || value.equals("false");
      case INT -> INTEGER.matcher(value).matches() && fits(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
      case BIGINT -> INTEGER.matcher(value).matches() && fits(value, Long.MIN_VALUE, Long.MAX_VALUE);
      case REAL, DOUBLE -> NUMBER.matcher(value).matches();
      case DECIMAL -> DECIMAL.matcher(value).matches();
      case TEXT -> true;
      case BYTES -> HEX.matcher(value).matches();
      case UUID -> UUID.matcher(value).matches();
      case TIMESTAMP -> parses(() -> Instant.parse(value));
      case DATE -> parses(() -> LocalDate.parse(value));
      case TIME -> parses(() -> LocalTime.parse(value));
      case JSON -> Json.valid(value);
    };
  }

  static String example(ColumnType type) {
    return switch (type) {
      case BOOLEAN -> " Use true or false.";
      case INT, BIGINT -> " Use an integer, such as 42.";
      case REAL, DOUBLE, DECIMAL -> " Use a number, such as 12.50.";
      case BYTES -> " Use hexadecimal digits, such as 0aff.";
      case UUID -> " Use a UUID, such as 123e4567-e89b-12d3-a456-426614174000.";
      case TIMESTAMP -> " Use an ISO 8601 instant, such as 2026-10-14T07:00:00Z.";
      case DATE -> " Use an ISO 8601 date, such as 2026-10-14.";
      case TIME -> " Use an ISO 8601 time, such as 07:00:00.";
      case JSON -> " Use a JSON document, such as {}.";
      case TEXT -> "";
    };
  }

  private static boolean fits(String value, long min, long max) {
    try {
      long number = Long.parseLong(value);
      return number >= min && number <= max;
    } catch (NumberFormatException tooLong) {
      return false;
    }
  }

  private static boolean parses(Runnable parse) {
    try {
      parse.run();
      return true;
    } catch (DateTimeParseException invalid) {
      return false;
    }
  }
}
