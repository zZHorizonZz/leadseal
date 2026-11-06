package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.contract.ContainerDatabase;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.database.postgresql.PostgreSQLParser;
import org.testcontainers.postgresql.PostgreSQLContainer;

final class PostgresTestDatabase extends ContainerDatabase {

  private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
      System.getProperty("leadseal.test.postgres.image", "postgres:17-alpine"));

  PostgresTestDatabase() {
    super(CONTAINER);
  }

  @Override
  public Dialect dialect() {
    return new PostgresDialect();
  }

  @Override
  public Parser flywayParser() {
    return new PostgreSQLParser(new ClassicConfiguration(), new ParsingContext());
  }

  @Override
  public Command trigger(TableName items, TableName log) {
    return sql -> {
      String insert = "  INSERT INTO " + sql.dialect().quote(log) + " (\"item\", \"note\") VALUES (NEW.\"id\", ";
      sql.append("CREATE FUNCTION \"items_log\"() RETURNS trigger LANGUAGE plpgsql AS ")
          .append(new PostgresDialect().dollarQuote("BEGIN\n" + insert + "'first');\n" + insert + "'second');\n"
              + "  RETURN NEW;\nEND;"))
          .end();
      sql.append("CREATE TRIGGER \"tg_items_log\" AFTER INSERT ON ").table(items)
          .append(" FOR EACH ROW EXECUTE FUNCTION \"items_log\"()");
    };
  }
}
