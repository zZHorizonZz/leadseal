package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

final class SqlServerTestDatabase implements TestDatabase {

  private static final MSSQLServerContainer CONTAINER = new MSSQLServerContainer(
      System.getProperty("leadseal.test.sqlserver.image", "mcr.microsoft.com/mssql/server:2022-latest"))
      .acceptLicense();
  private static final AtomicInteger DATABASES = new AtomicInteger();

  @Override
  public Dialect dialect() {
    return new SqlServerDialect();
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
    return new Database(CONTAINER.getJdbcUrl() + ";databaseName=" + name, CONTAINER.getUsername(),
        CONTAINER.getPassword());
  }
}
