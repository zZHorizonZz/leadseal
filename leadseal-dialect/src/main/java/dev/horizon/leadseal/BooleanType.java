package dev.horizon.leadseal;

/** A boolean. Values are {@code true} and {@code false}. */
public record BooleanType() implements Type {

  @Override
  public String zero() {
    return "false";
  }

  @Override
  public boolean accepts(String value) {
    return value.equals("true") || value.equals("false");
  }

  @Override
  public String example() {
    return "true";
  }

  @Override
  public String toString() {
    return "boolean";
  }
}
