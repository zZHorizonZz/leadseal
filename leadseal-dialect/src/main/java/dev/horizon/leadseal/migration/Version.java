package dev.horizon.leadseal.migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

record Version(@JsonValue String text) implements Comparable<Version> {

  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  Version {
  }

  static Version of(Instant time) {
    return new Version(LocalDateTime.ofInstant(time, ZoneOffset.UTC).withNano(0).format(FORMAT));
  }

  Version next(Version candidate) {
    return candidate.compareTo(this) > 0 ? candidate
        : new Version(LocalDateTime.parse(text, FORMAT).plusSeconds(1).format(FORMAT));
  }

  @Override
  public int compareTo(Version other) {
    return text.compareTo(other.text);
  }

  @Override
  public String toString() {
    return text;
  }
}
