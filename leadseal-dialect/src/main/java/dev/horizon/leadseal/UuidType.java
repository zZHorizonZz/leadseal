package dev.horizon.leadseal;

import java.util.Locale;
import java.util.UUID;

/** A UUID. Values look like {@code 123e4567-e89b-12d3-a456-426614174000}. */
public record UuidType() implements Type {

  @Override
  public String zero() {
    return "00000000-0000-0000-0000-000000000000";
  }

  @Override
  public boolean accepts(String value) {
    try {
      return UUID.fromString(value).toString().equals(value.toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException notAUuid) {
      return false;
    }
  }

  @Override
  public String example() {
    return "123e4567-e89b-12d3-a456-426614174000";
  }

  @Override
  public String toString() {
    return "UUID";
  }
}
