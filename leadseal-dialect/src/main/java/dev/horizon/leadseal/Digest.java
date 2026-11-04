package dev.horizon.leadseal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The SHA-256 digest of a text, which names things after their content, such as a snapshot or a long constraint.
 *
 * @param text the text
 */
public record Digest(String text) {

  /**
   * Gives the first hexadecimal digits of the digest.
   *
   * @param digits the number of digits, at most 64
   * @return the digits, for example {@code 3f2a9c1e}
   */
  public String hex(int digits) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, digits);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
