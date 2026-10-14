package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.contract.TestDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
  public String binaryCollation() {
    return "Latin1_General_BIN2";
  }

  @Override
  public boolean supportsIdentityChanges() {
    return false;
  }

  @Override
  public String tableComment(Connection connection, String table) throws SQLException {
    return description(connection, "SELECT CAST(value AS NVARCHAR(MAX)) FROM sys.extended_properties WHERE class = 1"
        + " AND name = N'MS_Description' AND major_id = OBJECT_ID(?) AND minor_id = 0", table);
  }

  @Override
  public String columnComment(Connection connection, String table, String column) throws SQLException {
    return description(connection, "SELECT CAST(value AS NVARCHAR(MAX)) FROM sys.extended_properties WHERE class = 1"
        + " AND name = N'MS_Description' AND major_id = OBJECT_ID(?) AND minor_id = COLUMNPROPERTY(OBJECT_ID(?), ?,"
        + " 'ColumnId')", table, table, column);
  }

  private static String description(Connection connection, String sql, String... params) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        statement.setString(i + 1, params[i]);
      }
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? result.getString(1) : null;
      }
    }
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
