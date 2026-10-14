package dev.horizon.leadseal.contract;

import dev.horizon.leadseal.Dialect;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public interface TestDatabase {

  Dialect dialect();

  Database create() throws SQLException;

  record Database(String url, String user, String password) {

    public Connection connect() throws SQLException {
      return DriverManager.getConnection(url, user, password);
    }
  }
}
