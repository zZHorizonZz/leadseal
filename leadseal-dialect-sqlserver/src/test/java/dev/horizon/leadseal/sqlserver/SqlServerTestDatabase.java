package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.contract.ContainerDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.database.sqlserver.SQLServerParser;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

final class SqlServerTestDatabase extends ContainerDatabase {

  private static final MSSQLServerContainer CONTAINER = new MSSQLServerContainer(
      System.getProperty("leadseal.test.sqlserver.image", "mcr.microsoft.com/mssql/server:2022-latest"))
      .acceptLicense();

  SqlServerTestDatabase() {
    super(CONTAINER);
  }

  @Override
  public Dialect dialect() {
    return new SqlServerDialect();
  }

  @Override
  public String binaryCollation() {
    return "Latin1_General_BIN2";
  }

  @Override
  public Parser flywayParser() {
    return new SQLServerParser(new ClassicConfiguration(), new ParsingContext());
  }

  @Override
  public Command trigger(TableName items, TableName log) {
    return sql -> {
      String insert = "  INSERT INTO " + sql.dialect().quote(log) + " ([item], [note]) SELECT [id], ";
      sql.isolate().append("CREATE TRIGGER [tg_items_log] ON ").table(items).append(" AFTER INSERT AS\nBEGIN\n"
          + "  SET NOCOUNT ON;\n" + insert + "N'first' FROM inserted;\n" + insert + "N'second' FROM inserted;\n"
          + "END");
    };
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

  @Override
  protected String url(String server, String database) {
    return server + ";databaseName=" + database;
  }

  private String description(Connection connection, String sql, String... params) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        statement.setString(i + 1, params[i]);
      }
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? result.getString(1) : null;
      }
    }
  }
}
