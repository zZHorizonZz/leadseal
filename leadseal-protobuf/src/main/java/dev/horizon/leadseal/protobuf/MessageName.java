package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

record MessageName(Descriptor message) {

  private static final Pattern VERSION = Pattern.compile("v\\d+((alpha|beta)\\d*)?");

  String table() {
    String name = message.getName();
    StringBuilder snake = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char letter = name.charAt(i);
      boolean boundary = i > 0 && Character.isUpperCase(letter) && (Character.isLowerCase(name.charAt(i - 1))
          || i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)));
      snake.append(boundary ? "_" : "").append(letter);
    }
    return snake.toString().toLowerCase(Locale.ROOT);
  }

  String id() {
    String packageName = message.getFile().getPackage();
    String nameInPackage = packageName.isEmpty() ? message.getFullName()
        : message.getFullName().substring(packageName.length() + 1);
    String stem = Arrays.stream(packageName.split("\\.")).filter(part -> !part.isEmpty())
        .filter(part -> !VERSION.matcher(part).matches()).collect(Collectors.joining("."));
    return stem.isEmpty() ? nameInPackage : stem + "." + nameInPackage;
  }
}
