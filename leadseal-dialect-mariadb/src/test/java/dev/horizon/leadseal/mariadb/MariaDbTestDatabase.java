package dev.horizon.leadseal.mariadb;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.contract.ContainerDatabase;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.database.mysql.mariadb.MariaDBParser;
import org.testcontainers.mariadb.MariaDBContainer;

final class MariaDbTestDatabase extends ContainerDatabase {

  private static final MariaDBContainer CONTAINER = new MariaDBContainer(
      System.getProperty("leadseal.test.mariadb.image", "mariadb:13.0")).withUsername("root").withPassword("test");

  MariaDbTestDatabase() {
    super(CONTAINER);
  }

  @Override
  public Dialect dialect() {
    return new MariaDbDialect();
  }

  @Override
  public String binaryCollation() {
    return "utf8mb4_bin";
  }

  @Override
  public Parser flywayParser() {
    return new MariaDBParser(new ClassicConfiguration(), new ParsingContext());
  }

  @Override
  public Command trigger(TableName items, TableName log) {
    return sql -> {
      String insert = "  INSERT INTO " + sql.dialect().quote(log) + " (`item`, `note`) VALUES (NEW.`id`, ";
      sql.append("CREATE TRIGGER `tg_items_log` AFTER INSERT ON ").table(items).append(" FOR EACH ROW BEGIN\n"
          + insert + "'first');\n" + insert + "'second');\nEND");
    };
  }
}
