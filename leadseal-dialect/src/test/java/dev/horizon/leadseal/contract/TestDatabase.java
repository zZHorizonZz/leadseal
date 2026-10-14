package dev.horizon.leadseal.contract;

import dev.horizon.leadseal.Dialect;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface TestDatabase {

  Dialect dialect();

  Database create() throws SQLException;

  String binaryCollation();

  default boolean supportsSchemas() {
    return true;
  }

  default boolean supportsPartialIndexes() {
    return true;
  }

  default boolean supportsIdentityChanges() {
    return true;
  }

  default boolean supportsComments() {
    return true;
  }

  default String tableComment(Connection connection, String table) throws SQLException {
    try (ResultSet result = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), table,
        null)) {
      return result.next() ? blank(result.getString("REMARKS")) : null;
    }
  }

  default String columnComment(Connection connection, String table, String column) throws SQLException {
    try (ResultSet result = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), table,
        column)) {
      return result.next() ? blank(result.getString("REMARKS")) : null;
    }
  }

  private static String blank(String text) {
    return text == null || text.isEmpty() ? null : text;
  }

  record Database(String url, String user, String password) {

    public Connection connect() throws SQLException {
      return DriverManager.getConnection(url, user, password);
    }
  }
}
