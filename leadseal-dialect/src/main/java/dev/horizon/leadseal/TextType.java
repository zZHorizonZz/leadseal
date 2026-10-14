package dev.horizon.leadseal;

/**
 * Text.
 *
 * @param length the maximum number of characters, or {@code null} for no limit
 */
public record TextType(Integer length) implements Type {

  public TextType {
    if (length != null && length <= 0) {
      throw new SchemaException("The length of a text type must be greater than 0, but is " + length + ".");
    }
  }

  @Override
  public String zero() {
    return "";
  }

  @Override
  public boolean accepts(String value) {
    return length == null || value.codePointCount(0, value.length()) <= length;
  }

  @Override
  public String example() {
    return "new";
  }

  @Override
  public String toString() {
    return length == null ? "text" : "text(" + length + ")";
  }
}
