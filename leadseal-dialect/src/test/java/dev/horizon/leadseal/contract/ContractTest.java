package dev.horizon.leadseal.contract;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.migration.MigrationDirectory;
import dev.horizon.leadseal.migration.MigrationOptions;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContractTest {

  protected static final Type BIGINT = new IntegerType(IntegerType.Size.INT64);

  protected Dialect dialect;
  protected Path migrations;
  private TestDatabase testDatabase;
  private TestDatabase.Database database;
  private Connection connection;
  private Instant now;

  protected abstract TestDatabase database();

  @BeforeAll
  void loadDatabase() {
    testDatabase = database();
    dialect = testDatabase.dialect();
  }

  @BeforeEach
  void createDatabase(@TempDir Path directory) throws SQLException {
    migrations = directory;
    database = testDatabase.create();
    connection = database.connect();
    now = Instant.parse("2026-10-14T07:00:00Z");
  }

  @AfterEach
  void closeDatabase() throws SQLException {
    connection.close();
  }

  protected void generate(Schema schema) {
    now = now.plus(Duration.ofMinutes(1));
    new MigrationDirectory(migrations, List.of(dialect),
        MigrationOptions.defaults().withClock(Clock.fixed(now, ZoneOffset.UTC)).allowUnsafe())
        .generate(schema);
  }

  protected int migrate() {
    String location = "filesystem:" + migrations.toAbsolutePath().toString().replace('\\', '/') + "/**/"
        + dialect.name();
    int executed = Flyway.configure().dataSource(database.url(), database.user(), database.password())
        .locations(location).load().migrate().migrationsExecuted;
    try {
      connection.close();
      connection = database.connect();
    } catch (SQLException failed) {
      throw new IllegalStateException(failed);
    }
    return executed;
  }

  protected void apply(Schema schema) {
    generate(schema);
    migrate();
  }

  protected String q(String identifier) {
    return dialect.quote(identifier);
  }

  protected TestDatabase testDatabase() {
    return testDatabase;
  }

  protected List<List<String>> query(String sql, Object... params) throws SQLException {
    List<List<String>> rows = new ArrayList<>();
    try (PreparedStatement statement = prepare(sql, params); ResultSet result = statement.executeQuery()) {
      int count = result.getMetaData().getColumnCount();
      while (result.next()) {
        List<String> row = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
          row.add(result.getString(i));
        }
        rows.add(row);
      }
    }
    return rows;
  }

  protected static Column column(String id, String name, Type type) {
    return Column.of(id, name, type, NotNull.INSTANCE);
  }

  protected static Column text(String id, String name, int length) {
    return column(id, name, TextType.of(length));
  }

  protected static Table table(String id, String name, List<Column> columns, List<String> key,
      List<? extends TableAttribute> indexes, List<? extends TableAttribute> keys) {
    List<TableAttribute> all = new ArrayList<>();
    all.add(PrimaryKey.of(name, key));
    all.addAll(indexes);
    all.addAll(keys);
    return new Table(id, name, columns, all);
  }

  protected static Table table(String id, String name, List<Column> columns, String... key) {
    return table(id, name, columns, List.of(key), List.of(), List.of());
  }

  protected static Schema schema(Table... tables) {
    return Schema.of(tables);
  }

  private PreparedStatement prepare(String sql, Object... params) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql);
    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
    return statement;
  }
}
