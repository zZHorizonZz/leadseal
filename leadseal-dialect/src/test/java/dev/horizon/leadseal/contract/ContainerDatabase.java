package dev.horizon.leadseal.contract;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.JdbcDatabaseContainer;

public abstract class ContainerDatabase implements TestDatabase {

  private static final AtomicInteger DATABASES = new AtomicInteger();

  private final JdbcDatabaseContainer<?> container;

  protected ContainerDatabase(JdbcDatabaseContainer<?> container) {
    this.container = container;
  }

  @Override
  public Database create() throws SQLException {
    synchronized (container) {
      if (!container.isRunning()) {
        container.start();
      }
    }
    String name = "test_" + DATABASES.incrementAndGet();
    try (Connection connection = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(),
        container.getPassword()); Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE " + name);
    }
    return new Database(url(container.getJdbcUrl(), name), container.getUsername(), container.getPassword());
  }

  protected String url(String server, String database) {
    return server.replaceFirst("/[^/?]+(\\?|$)", "/" + database + "$1");
  }
}
