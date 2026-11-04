package dev.horizon.leadseal.type;

/**
 * A text.
 *
 * @param length the largest number of characters
 */
public record TextType(Length length) implements Type {

  public TextType {
    length = length == null ? Length.UNLIMITED : length;
  }

  /**
   * Gives a text of any length.
   *
   * @return the type
   */
  public static TextType unlimited() {
    return new TextType(Length.UNLIMITED);
  }

  /**
   * Gives a text with a limit.
   *
   * @param characters the largest number of characters
   * @return the type
   */
  public static TextType of(int characters) {
    return new TextType(new Length.Limit(characters));
  }

  @Override
  public String zero() {
    return "";
  }

  @Override
  public boolean accepts(String value) {
    return length.fits(value.codePointCount(0, value.length()));
  }

  @Override
  public String example() {
    return "new";
  }

  @Override
  public boolean collates() {
    return true;
  }

  @Override
  public String toString() {
    return length.map(characters -> "text(" + characters + ")", () -> "text");
  }
}
