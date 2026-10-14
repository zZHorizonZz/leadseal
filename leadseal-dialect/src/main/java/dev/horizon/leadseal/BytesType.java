package dev.horizon.leadseal;

import java.util.HexFormat;

/**
 * Binary data. Values are hexadecimal, for example {@code 0aff}.
 *
 * @param length the maximum number of bytes, or {@code null} for no limit
 */
public record BytesType(Integer length) implements Type {

  public BytesType {
    if (length != null && length <= 0) {
      throw new SchemaException("The length of a bytes type must be greater than 0, but is " + length + ".");
    }
  }

  @Override
  public String zero() {
    return "";
  }

  @Override
  public boolean accepts(String value) {
    try {
      byte[] bytes = HexFormat.of().parseHex(value);
      return length == null || bytes.length <= length;
    } catch (IllegalArgumentException notHexadecimal) {
      return false;
    }
  }

  @Override
  public String example() {
    return "0aff";
  }

  @Override
  public String toString() {
    return length == null ? "bytes" : "bytes(" + length + ")";
  }
}
