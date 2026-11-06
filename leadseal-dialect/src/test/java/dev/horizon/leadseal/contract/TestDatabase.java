package dev.horizon.leadseal.contract;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.internal.parser.Parser;

public interface TestDatabase {

  Dialect dialect();

  Database create() throws SQLException;

  Parser flywayParser();

  Command trigger(TableName items, TableName log);

  record Database(String url, String user, String password) {

    public Connection connect() throws SQLException {
      return DriverManager.getConnection(url, user, password);
    }
  }
}
