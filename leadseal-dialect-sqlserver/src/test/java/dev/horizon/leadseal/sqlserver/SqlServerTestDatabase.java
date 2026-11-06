package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.contract.ContainerDatabase;
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
  protected String url(String server, String database) {
    return server + ";databaseName=" + database;
  }
}
