package dev.horizon.leadseal.migration;

import dev.horizon.leadseal.change.Change;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

record MigrationName(List<Change> changes) {

  private static final int MAX_LENGTH = 60;
  private static final String FALLBACK = "change";

  String text() {
    List<String> summaries = summaries();
    String name = switch (summaries.size()) {
      case 0 -> FALLBACK;
      case 1 -> summaries.getFirst();
      case 2 -> summaries.get(0) + "_and_" + summaries.get(1);
      default -> summaries.getFirst() + "_and_" + (summaries.size() - 1) + "_more";
    };
    String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    return clean.length() <= MAX_LENGTH ? clean : clean.substring(0, MAX_LENGTH).replaceAll("_$", "");
  }

  private List<String> summaries() {
    Set<String> parts = new LinkedHashSet<>();
    changes.stream().filter(change -> !change.minor()).forEach(change -> parts.add(change.summary()));
    if (parts.isEmpty()) {
      changes.forEach(change -> parts.add(change.summary()));
    }
    return List.copyOf(parts);
  }
}
