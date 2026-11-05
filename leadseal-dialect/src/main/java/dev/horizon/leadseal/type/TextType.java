package dev.horizon.leadseal.type;

/**
 * A text.
 *
 * @param length the largest number of characters
 */
public record TextType(Length length) implements Type {

  private static final int UUID_LENGTH = 36;

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

  @Override
  public boolean isAssignableFrom(Type origin) {
    return switch (origin) {
      case TextType(Length other) -> other.map(length::fits, () -> length.fits(Integer.MAX_VALUE));
      case EnumType values -> values.literals().stream()
          .allMatch(literal -> length.fits(literal.codePointCount(0, literal.length())));
      case UuidType _ -> length.fits(UUID_LENGTH);
      default -> false;
    };
  }
}
