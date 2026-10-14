package dev.horizon.leadseal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** A JSON document. */
public record JsonType() implements Type {

  private static final ObjectMapper PARSER = new ObjectMapper();

  @Override
  public String zero() {
    return "{}";
  }

  @Override
  public boolean accepts(String value) {
    try {
      PARSER.readTree(value);
      return true;
    } catch (JsonProcessingException notJson) {
      return false;
    }
  }

  @Override
  public String example() {
    return "{}";
  }

  @Override
  public String toString() {
    return "JSON";
  }
}
