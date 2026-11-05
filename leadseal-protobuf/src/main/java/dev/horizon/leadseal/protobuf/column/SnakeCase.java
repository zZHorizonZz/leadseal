package dev.horizon.leadseal.protobuf.column;

import java.util.Locale;

/**
 * A protobuf name in snake case, such as {@code order_line} for {@code OrderLine}.
 *
 * @param name the name in camel case
 */
public record SnakeCase(String name) {

  /**
   * Gives the name in snake case.
   *
   * @return the name, in lower case with an underscore between words
   */
  public String value() {
    StringBuilder snake = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char letter = name.charAt(i);
      boolean boundary = i > 0 && Character.isUpperCase(letter) && (Character.isLowerCase(name.charAt(i - 1))
          || i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)));
      snake.append(boundary ? "_" : "").append(letter);
    }
    return snake.toString().toLowerCase(Locale.ROOT);
  }
}
