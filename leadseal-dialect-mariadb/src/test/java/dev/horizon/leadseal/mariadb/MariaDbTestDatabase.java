package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.mariadb.MariaDBContainer;

final class MariaDbTestDatabase implements TestDatabase {

  private static final MariaDBContainer CONTAINER = new MariaDBContainer(
      System.getProperty("leadseal.test.mariadb.image", "mariadb:13.0")).withUsername("root").withPassword("test");
  private static final AtomicInteger DATABASES = new AtomicInteger();

  @Override
  public Dialect dialect() {
    return new MariaDbDialect();
  }

  @Override
  public String binaryCollation() {
    return "utf8mb4_bin";
  }

  @Override
  public boolean supportsPartialIndexes() {
    return false;
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
