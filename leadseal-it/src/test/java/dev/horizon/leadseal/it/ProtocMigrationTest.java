package dev.horizon.leadseal.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProtocMigrationTest {

  private static final Path MIGRATIONS = Path.of(System.getProperty("leadseal.migrations"));

  private static List<Path> steps() throws IOException {
    try (Stream<Path> folders = Files.list(MIGRATIONS)) {
      return folders.filter(folder -> Files.exists(folder.resolve("migration.json"))).sorted().toList();
    }
  }

  private static String version(Path step) {
    return step.getFileName().toString().split("_", 2)[0];
  }

  private static int migrate(Database database, Database.Location location, Path step) {
    String folders = MIGRATIONS.toAbsolutePath().toString().replace('\\', '/');
    return Flyway.configure().dataSource(location.url(), location.user(), location.password())
        .locations("filesystem:" + folders + "/**/" + database.dialect()).target(version(step)).load()
        .migrate().migrationsExecuted;
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
  void writesAMigrationForEachChangedStepAndDialect() throws Exception {
    List<Path> steps = steps();

    assertThat(steps).hasSize(3);
    for (Path step : steps) {
      String[] folder = step.getFileName().toString().split("_", 2);
      for (Database database : Database.values()) {
        assertThat(step.resolve(database.dialect()).resolve("V" + folder[0] + "__" + folder[1] + ".sql")).exists();
      }
    }
  }

  @ParameterizedTest
  @EnumSource(Database.class)
  void appliesEachStepAndKeepsTheRows(Database database) throws Exception {
    Database.Location location = database.create();
    List<Path> steps = steps();

    assertThat(migrate(database, location, steps.get(0))).isEqualTo(1);
    execute(location, "INSERT INTO customers (id, email) VALUES (1, 'ada@example.com')");
    execute(location, "INSERT INTO orders (id, customer_id, state) VALUES (1, 1, 'new')");

    assertThat(migrate(database, location, steps.get(1))).isEqualTo(1);
    assertThat(query(location, "SELECT email_address FROM customers")).containsExactly(List.of("ada@example.com"));
    assertThat(query(location, "SELECT quantity, note FROM orders")).containsExactly(Arrays.asList("1", null));
    execute(location, "INSERT INTO order_line (order_id, line, product) VALUES (1, 1, 'pen')");

    assertThat(migrate(database, location, steps.get(2))).isEqualTo(1);
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
    assertThat(migrate(database, location, steps.get(2))).isZero();
  }
}
