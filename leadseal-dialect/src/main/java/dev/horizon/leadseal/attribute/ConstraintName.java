package dev.horizon.leadseal.attribute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * A generated name of a key, an index or a constraint: a prefix, the table name and the column names, such as
 * {@code ix_orders_state}. A name longer than 63 characters is shortened and ends with a hash of the full name, so that
 * every database accepts it.
 *
 * @param prefix the prefix, such as {@code ix}
 * @param table the table name
 * @param parts the column names or other parts, in order
 */
public record ConstraintName(String prefix, String table, List<String> parts) {

  private static final int MAX_LENGTH = 63;
  private static final int HASH_LENGTH = 8;

  public ConstraintName {
    parts = List.copyOf(parts);
  }

  /**
   * Returns the name.
   *
   * @return the name, at most 63 characters long
   */
  public String value() {
    String name = prefix + "_" + table + (parts.isEmpty() ? "" : "_" + String.join("_", parts));
    if (name.length() <= MAX_LENGTH) {
      return name;
    }
    return name.substring(0, MAX_LENGTH - HASH_LENGTH - 1) + "_" + hash(name);
  }

  /**
   * Returns a name that ends with a hash of a text, for a constraint that has no column names, such as a check.
   *
   * @param prefix the prefix, such as {@code ck}
   * @param table the table name
   * @param text the text to hash, such as the check expression
   * @return the name
   */
  public static ConstraintName hashed(String prefix, String table, String text) {
    return new ConstraintName(prefix, table, List.of(hash(text)));
  }

  private static String hash(String text) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
    } catch (NoSuchAlgorithmException missing) {
      throw new IllegalStateException(missing);
    }
  }
}
