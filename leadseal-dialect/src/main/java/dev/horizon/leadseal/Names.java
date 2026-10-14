package dev.horizon.leadseal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Checks and generates identifiers. */
final class Names {

  static final int MAX_LENGTH = 63;
  private static final int HASH_LENGTH = 8;

  private Names() {
  }

  static void require(String value, String what) {
    if (value == null || value.isBlank()) {
      throw new SchemaException("The " + what + " is empty.");
    }
  }

  static String generate(String prefix, String table, List<String> columns) {
    String name = prefix + "_" + table + (columns.isEmpty() ? "" : "_" + String.join("_", columns));
    if (name.length() <= MAX_LENGTH) {
      return name;
    }
    return name.substring(0, MAX_LENGTH - HASH_LENGTH - 1) + "_" + hash(name).substring(0, HASH_LENGTH);
  }

  static String hash(String text) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException missing) {
      throw new IllegalStateException(missing);
    }
  }
}
