package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.contract.TestDatabase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.database.sqlite.SQLiteParser;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;

final class SqliteTestDatabase implements TestDatabase {

  @Override
  public Parser flywayParser() {
    return new SQLiteParser(new ClassicConfiguration(), new ParsingContext());
  }

  @Override
  public Command trigger(TableName items, TableName log) {
    return sql -> {
      String insert = "  INSERT INTO " + sql.dialect().quote(log) + " (\"item\", \"note\") VALUES (NEW.\"id\", ";
      sql.append("CREATE TRIGGER \"tg_items_log\" AFTER INSERT ON ").table(items).append(" BEGIN\n" + insert
          + "'first');\n" + insert + "'second');\nEND");
    };
  }

  @Override
  public Dialect dialect() {
    return new SqliteDialect();
  }

  @Override
  public String binaryCollation() {
    return "BINARY";
  }

  @Override
  public Database create() {
    try {
      Path file = Files.createTempFile("leadseal", ".db");
      file.toFile().deleteOnExit();
      return new Database("jdbc:sqlite:" + file + "?foreign_keys=true", null, null);
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }
}
