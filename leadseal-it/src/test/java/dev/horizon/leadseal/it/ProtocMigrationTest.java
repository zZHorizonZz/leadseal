package dev.horizon.leadseal.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProtocMigrationTest {

  private static final Path PROTOC = Path.of(System.getProperty("leadseal.protoc"));
  private static final Path PLUGIN = Path.of(System.getProperty("leadseal.plugin"));
  private static final Path INCLUDE = Path.of(System.getProperty("leadseal.include"));
  private static final Path PROTOS = Path.of(System.getProperty("leadseal.protos"));
  private static final String DIALECTS = "dialect=postgresql,dialect=mariadb,dialect=sqlserver,dialect=sqlite";

  @TempDir
  Path migrations;

  record Run(int exitCode, String errors) {
  }

  @BeforeAll
  static void makeExecutable() {
    assertThat(PROTOC.toFile().setExecutable(true)).isTrue();
    assertThat(PLUGIN.toFile().setExecutable(true)).isTrue();
  }

  private Run protoc(String step, String options) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(PROTOC.toString(), "-I" + PROTOS.resolve(step), "-I" + INCLUDE,
        "--plugin=protoc-gen-leadseal=" + PLUGIN, "--leadseal_out=.", "--leadseal_opt=" + options,
        "shop/v1/shop.proto").directory(migrations.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new Run(process.waitFor(), errors);
  }

  private void generate(String step) throws IOException, InterruptedException {
    Run run = protoc(step, "migrations=.," + DIALECTS);
    assertThat(run.exitCode()).as(run.errors()).isZero();
  }

  private List<Path> steps() throws IOException {
    try (Stream<Path> folders = Files.list(migrations)) {
      return folders.filter(folder -> Files.exists(folder.resolve("migration.json"))).sorted().toList();
    }
  }

  private int migrate(Database database, Database.Location location) {
    String folders = migrations.toAbsolutePath().toString().replace('\\', '/');
    return Flyway.configure().dataSource(location.url(), location.user(), location.password())
        .locations("filesystem:" + folders + "/**/" + database.dialect()).load().migrate().migrationsExecuted;
  }

  private static List<List<String>> query(Database.Location location, String sql) throws SQLException {
    List<List<String>> rows = new ArrayList<>();
    try (Connection connection = location.connect();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        List<String> row = new ArrayList<>();
        for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
          row.add(result.getString(i));
        }
        rows.add(row);
      }
    }
    return rows;
  }

  private static void execute(Database.Location location, String sql) throws SQLException {
    try (Connection connection = location.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private static List<String> columns(Database.Location location, String table) throws SQLException {
    List<String> columns = new ArrayList<>();
    try (Connection connection = location.connect();
        ResultSet result = connection.getMetaData()
            .getColumns(connection.getCatalog(), connection.getSchema(), table, null)) {
      while (result.next()) {
        columns.add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
      }
    }
    return columns;
  }

  @Test
  void writesAMigrationForEachStepAndDialect() throws Exception {
    generate("step1");
    generate("step2");
    generate("step3");

    List<Path> steps = steps();
    assertThat(steps).hasSize(3);
    for (Path step : steps) {
      String name = step.getFileName().toString();
      String version = name.substring(0, "yyyyMMddTHHmmss".length()).replace("T", "");
      for (Database database : Database.values()) {
        assertThat(step.resolve(database.dialect()).resolve("V" + version + "__" + name.substring(16) + ".sql"))
            .exists();
      }
    }
  }

  @Test
  void writesNothingWhenTheSchemaDidNotChange() throws Exception {
    generate("step1");

    generate("step1");

    assertThat(steps()).hasSize(1);
  }

  @Test
  void reportsInvalidOptionsThroughProtoc() throws Exception {
    Run run = protoc("step1", "migrations=.,dialect=oracle");

    assertThat(run.exitCode()).isNotZero();
    assertThat(run.errors()).contains("No installed dialect is named 'oracle'");
  }

  @ParameterizedTest
  @EnumSource(Database.class)
  void appliesEachStepAndKeepsTheRows(Database database) throws Exception {
    Database.Location location = database.create();

    generate("step1");
    assertThat(migrate(database, location)).isEqualTo(1);
    execute(location, "INSERT INTO customers (id, email) VALUES (1, 'ada@example.com')");
    execute(location, "INSERT INTO orders (id, customer_id, state) VALUES (1, 1, 'new')");

    generate("step2");
    assertThat(migrate(database, location)).isEqualTo(1);
    assertThat(query(location, "SELECT email_address FROM customers")).containsExactly(List.of("ada@example.com"));
    assertThat(query(location, "SELECT quantity, note FROM orders")).containsExactly(Arrays.asList("1", null));
    execute(location, "INSERT INTO order_line (order_id, line, product) VALUES (1, 1, 'pen')");

    generate("step3");
    assertThat(migrate(database, location)).isEqualTo(1);
    assertThat(columns(location, "customers")).isEmpty();
    assertThat(columns(location, "orders")).containsExactly("id", "customer_id", "state", "created", "quantity");
    assertThat(query(location, "SELECT email_address FROM clients")).containsExactly(List.of("ada@example.com"));
    assertThat(query(location, "SELECT product FROM order_line WHERE added IS NOT NULL"))
        .containsExactly(List.of("pen"));
    execute(location, "UPDATE orders SET state = '" + "x".repeat(50) + "', quantity = 5000000000");
    assertThat(query(location, "SELECT quantity FROM orders")).containsExactly(List.of("5000000000"));

    execute(location, "DELETE FROM clients");

    assertThat(query(location, "SELECT id FROM orders")).isEmpty();
    assertThat(query(location, "SELECT order_id FROM order_line")).isEmpty();
    assertThat(migrate(database, location)).isZero();
  }
}
