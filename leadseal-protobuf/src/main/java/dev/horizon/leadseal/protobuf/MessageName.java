package dev.horizon.leadseal.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import dev.horizon.leadseal.protobuf.column.SnakeCase;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

record MessageName(Descriptor message) {

  private static final Pattern VERSION = Pattern.compile("v\\d+((alpha|beta)\\d*)?");

  String table() {
    return new SnakeCase(message.getName()).value();
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
