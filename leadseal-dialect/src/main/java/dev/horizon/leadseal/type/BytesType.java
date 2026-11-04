package dev.horizon.leadseal.type;

import java.util.HexFormat;

/**
 * Bytes.
 *
 * @param length the largest number of bytes
 */
public record BytesType(Length length) implements Type {

  public BytesType {
    length = length == null ? Length.UNLIMITED : length;
  }

  /**
   * Gives bytes of any length.
   *
   * @return the type
   */
  public static BytesType unlimited() {
    return new BytesType(Length.UNLIMITED);
  }

  /**
   * Gives bytes with a limit.
   *
   * @param bytes the largest number of bytes
   * @return the type
   */
  public static BytesType of(int bytes) {
    return new BytesType(new Length.Limit(bytes));
  }

  @Override
  public String zero() {
    return "";
  }

  @Override
  public boolean accepts(String value) {
    try {
      return length.fits(HexFormat.of().parseHex(value).length);
    } catch (IllegalArgumentException _) {
      return false;
    }
  }

  @Override
  public String example() {
    return "0aff";
  }

  @Override
  public String toString() {
    return length.map(bytes -> "bytes(" + bytes + ")", () -> "bytes");
  }
}
