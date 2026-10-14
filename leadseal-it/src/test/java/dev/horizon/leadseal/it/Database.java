package dev.horizon.leadseal.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
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
      .withUsername("root").withPassword("test")),
  SQLSERVER("sqlserver", () -> new MSSQLServerContainer(System.getProperty("leadseal.test.sqlserver.image",
      "mcr.microsoft.com/mssql/server:2022-latest")).acceptLicense()),
  SQLITE("sqlite", null);

  private static final AtomicInteger DATABASES = new AtomicInteger();

  private final String dialect;
  private final Supplier<JdbcDatabaseContainer<?>> containers;
  private JdbcDatabaseContainer<?> container;

  Database(String dialect, Supplier<JdbcDatabaseContainer<?>> containers) {
    this.dialect = dialect;
    this.containers = containers;
  }

  record Location(String url, String user, String password) {

    Connection connect() throws SQLException {
      return DriverManager.getConnection(url, user, password);
    }
  }

  String dialect() {
    return dialect;
  }

  synchronized Location create() throws SQLException {
    String name = "it_" + DATABASES.incrementAndGet();
    if (containers == null) {
      return new Location("jdbc:sqlite:" + file(name) + "?foreign_keys=true", null, null);
    }
    if (container == null) {
      container = containers.get();
      container.start();
    }
    try (Connection admin = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(),
        container.getPassword()); Statement statement = admin.createStatement()) {
      statement.execute("CREATE DATABASE " + name);
    }
    String url = this == SQLSERVER ? container.getJdbcUrl() + ";databaseName=" + name
        : container.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + name + "$1");
    return new Location(url, container.getUsername(), container.getPassword());
  }

  private static Path file(String name) {
    try {
      Path file = Files.createTempFile(name, ".db");
      file.toFile().deleteOnExit();
      return file;
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }
}
