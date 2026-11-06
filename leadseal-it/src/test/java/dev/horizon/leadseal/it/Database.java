package dev.horizon.leadseal.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

enum Database {

  POSTGRESQL("postgresql", () -> new PostgreSQLContainer(System.getProperty("leadseal.test.postgres.image",
      "postgres:17-alpine"))),
  MARIADB("mariadb", () -> new MariaDBContainer(System.getProperty("leadseal.test.mariadb.image", "mariadb:13.0"))
      .withUsername("root").withPassword("test")) {

    @Override
    Place place(Connection connection, String name) throws SQLException {
      String[] parts = name.split("\\.");
      return parts.length == 2 ? new Place(parts[0], null, parts[1]) : super.place(connection, name);
    }

    @Override
    void dropNamespace(Connection connection, String name) throws SQLException {
      try (Statement statement = connection.createStatement()) {
        statement.execute("SET FOREIGN_KEY_CHECKS = 0");
        statement.execute("DROP DATABASE IF EXISTS " + name);
      }
    }
  },
  SQLSERVER("sqlserver", () -> new MSSQLServerContainer(System.getProperty("leadseal.test.sqlserver.image",
      "mcr.microsoft.com/mssql/server:2022-latest")).acceptLicense()) {

    @Override
    String comment(Connection connection, Place table, String column) throws SQLException {
      String target = table.schema() == null ? table.table() : table.schema() + "." + table.table();
      String sql = "SELECT CAST(value AS NVARCHAR(MAX)) FROM sys.extended_properties WHERE class = 1 AND name ="
          + " N'MS_Description' AND major_id = OBJECT_ID(?) AND minor_id = "
          + (column == null ? "0" : "COLUMNPROPERTY(OBJECT_ID(?), ?, 'ColumnId')");
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, target);
        if (column != null) {
          statement.setString(2, target);
          statement.setString(3, column);
        }
        try (ResultSet result = statement.executeQuery()) {
          return result.next() ? result.getString(1) : null;
        }
      }
    }
  },
  SQLITE("sqlite", null);

  private static final AtomicInteger DATABASES = new AtomicInteger();

  private final String dialect;
  private final Supplier<JdbcDatabaseContainer<?>> containers;
  private JdbcDatabaseContainer<?> container;

  Database(String dialect, Supplier<JdbcDatabaseContainer<?>> containers) {
    this.dialect = dialect;
    this.containers = containers;
  }

  record Place(String catalog, String schema, String table) {
  }

  Place place(Connection connection, String name) throws SQLException {
    String[] parts = name.split("\\.");
    return parts.length == 2 ? new Place(connection.getCatalog(), parts[0], parts[1])
        : new Place(connection.getCatalog(), connection.getSchema(), name);
  }

  String comment(Connection connection, Place table, String column) throws SQLException {
    try (ResultSet result = column == null
        ? connection.getMetaData().getTables(table.catalog(), table.schema(), table.table(), null)
        : connection.getMetaData().getColumns(table.catalog(), table.schema(), table.table(), column)) {
      String remarks = result.next() ? result.getString("REMARKS") : null;
      return remarks == null || remarks.isEmpty() ? null : remarks;
    }
  }

  void dropNamespace(Connection connection, String name) throws SQLException {
  }

  record Location(String url, String user, String password) {

    Connection connect() throws SQLException {
      return DriverManager.getConnection(url, user, password);
    }
  }

  String dialect() {
    return dialect;
  }

  synchronized void start() {
    if (containers != null && container == null) {
      container = containers.get();
      container.start();
    }
  }

  synchronized Location create() throws SQLException {
    String name = "it_" + DATABASES.incrementAndGet();
    if (containers == null) {
      return new Location("jdbc:sqlite:" + file(name) + "?foreign_keys=true", null, null);
    }
    start();
    try (Connection admin = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(),
        container.getPassword()); Statement statement = admin.createStatement()) {
      statement.execute("CREATE DATABASE " + name);
    }
    String url = this == SQLSERVER ? container.getJdbcUrl() + ";databaseName=" + name
        : container.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + name + "$1");
    return new Location(url, container.getUsername(), container.getPassword());
  }

  private Path file(String name) {
    try {
      Path file = Files.createTempFile(name, ".db");
      file.toFile().deleteOnExit();
      return file;
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }
}
