package dev.horizon.leadseal.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.SchemaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record Behavior(String suite, String name, Set<String> databases, List<Behavior.Step> steps) {

  private static final Pattern DIRECTIVE = Pattern.compile("--\\s*(databases|migrate|only|error|expect):\\s*(.*)");

  static Behavior read(Path root, Path file) throws IOException {
    String suite = root.relativize(file.getParent()).toString().replace('\\', '/');
    String name = file.getFileName().toString().replaceFirst("\\.sql$", "");
    List<String> lines = Files.readAllLines(file);
    List<Step> steps = new ArrayList<>();
    Set<String> databases = Set.of();
    Set<String> only = Set.of();
    StringBuilder statement = new StringBuilder();
    String start = "";
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).strip();
      String place = suite + "/" + file.getFileName() + ":" + (i + 1);
      Matcher directive = DIRECTIVE.matcher(line);
      if (!statement.isEmpty()) {
        statement.append('\n').append(line);
      } else if (directive.matches()) {
        String value = directive.group(2).strip();
        switch (directive.group(1)) {
          case "databases" -> {
            if (!steps.isEmpty()) {
              throw new IllegalStateException(place + ": -- databases: must come before the steps");
            }
            databases = Set.of(value.split("\\s*,\\s*"));
          }
          case "only" -> {
            only = Set.of(value.split("\\s*,\\s*"));
            continue;
          }
          case "migrate" -> {
            String[] parts = value.split("\\s+");
            steps.add(new Migrate(place, parts[0], parts.length > 1 && parts[1].equals("allow_unsafe")).within(only));
          }
          case "error" -> {
            if (steps.isEmpty()) {
              throw new IllegalStateException(place + ": -- error: must follow a statement or a migration");
            }
            steps.add(steps.removeLast().refuse(place, value));
          }
          case "expect" -> {
            List<List<String>> rows = new ArrayList<>();
            while (i + 1 < lines.size() && !lines.get(i + 1).isBlank() && !lines.get(i + 1).strip().startsWith("--")) {
              rows.add(Arrays.stream(lines.get(++i).strip().split("\\s*\\|\\s*", -1)).map(cell -> switch (cell) {
                case "NULL" -> null;
                case "''" -> "";
                default -> cell;
              }).toList());
            }
            steps.add(new Expect(place, value, rows).within(only));
          }
          default -> throw new IllegalStateException(place + ": unknown directive " + directive.group(1));
        }
        only = Set.of();
        continue;
      } else if (line.isEmpty() || line.startsWith("--")) {
        continue;
      } else {
        statement.append(line);
        start = place;
      }
      if (line.endsWith(";")) {
        steps.add(new Execute(start, statement.substring(0, statement.length() - 1)).within(only));
        statement.setLength(0);
        only = Set.of();
      }
    }
    if (!statement.isEmpty()) {
      throw new IllegalStateException(start + ": the statement has no semicolon at its end");
    }
    if (steps.isEmpty() || !(steps.getFirst() instanceof Migrate)) {
      throw new IllegalStateException(suite + "/" + file.getFileName() + ": start with -- migrate: <version>");
    }
    return new Behavior(suite, name, databases, steps);
  }

  boolean runs(Database database) {
    return databases.isEmpty() || databases.contains(database.dialect());
  }

  void run(Database database, Cases cases) throws Exception {
    try (Session session = new Session(database, cases, suite)) {
      for (Step step : steps) {
        step.run(session);
      }
    }
  }

  sealed interface Step {

    void run(Session session) throws Exception;

    default Step within(Set<String> databases) {
      return databases.isEmpty() ? this : new Only(databases, this);
    }

    default Step refuse(String place, String message) {
      throw new IllegalStateException(place + ": -- error: must follow a statement or a migration");
    }
  }

  record Migrate(String place, String version, boolean unsafe) implements Step {

    @Override
    public void run(Session session) throws SQLException {
      try {
        session.migrate(version, unsafe);
      } catch (SchemaException failure) {
        throw new AssertionError(place + ": " + failure.getMessage(), failure);
      }
    }

    @Override
    public Step refuse(String place, String message) {
      return new RefuseMigration(this.place, version, unsafe, message);
    }
  }

  record RefuseMigration(String place, String version, boolean unsafe, String message) implements Step {

    @Override
    public void run(Session session) {
      assertThatThrownBy(() -> session.migrate(version, unsafe)).as(place).isInstanceOf(SchemaException.class)
          .hasMessageContaining(message);
    }
  }

  record Execute(String place, String sql) implements Step {

    @Override
    public void run(Session session) {
      try {
        session.execute(sql);
      } catch (SQLException failure) {
        throw new AssertionError(place + ": " + failure.getMessage(), failure);
      }
    }

    @Override
    public Step refuse(String place, String message) {
      return new Refuse(this.place, sql, message);
    }
  }

  record Refuse(String place, String sql, String message) implements Step {

    @Override
    public void run(Session session) {
      assertThatThrownBy(() -> session.execute(sql)).as(place).isInstanceOf(SQLException.class)
          .hasMessageContaining(message);
    }
  }

  record Expect(String place, String query, List<List<String>> rows) implements Step {

    @Override
    public void run(Session session) throws SQLException {
      assertThat(session.query(query)).as(place + ": " + query).containsExactlyElementsOf(rows);
    }
  }

  record Only(Set<String> databases, Step step) implements Step {

    @Override
    public void run(Session session) throws Exception {
      if (databases.contains(session.dialect())) {
        step.run(session);
      }
    }

    @Override
    public Step refuse(String place, String message) {
      return new Only(databases, step.refuse(place, message));
    }
  }
}
