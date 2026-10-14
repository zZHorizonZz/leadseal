package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.postgresql.PostgreSQLContainer;

final class PostgresTestDatabase implements TestDatabase {

  private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
      System.getProperty("leadseal.test.postgres.image", "postgres:17-alpine"));
  private static final AtomicInteger DATABASES = new AtomicInteger();

  @Override
  public Dialect dialect() {
    return new PostgresDialect();
  }

  @Override
  public String binaryCollation() {
    return "C";
  }

  @Override
  public Database create() throws SQLException {
    synchronized (CONTAINER) {
      if (!CONTAINER.isRunning()) {
        CONTAINER.start();
      }
    }
    String name = "test_" + DATABASES.incrementAndGet();
    try (Connection connection = DriverManager.getConnection(CONTAINER.getJdbcUrl(), CONTAINER.getUsername(),
        CONTAINER.getPassword()); Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE " + name);
    }
    String url = CONTAINER.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + name + "$1");
    return new Database(url, CONTAINER.getUsername(), CONTAINER.getPassword());
  }
}
