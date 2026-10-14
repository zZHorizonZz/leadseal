package dev.horizon.leadseal.contract;

import dev.horizon.leadseal.BooleanType;
import dev.horizon.leadseal.BytesType;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.DateType;
import dev.horizon.leadseal.DecimalType;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.FloatType;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.JsonType;
import dev.horizon.leadseal.MigrationOptions;
import dev.horizon.leadseal.Migrations;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TextType;
import dev.horizon.leadseal.TimeType;
import dev.horizon.leadseal.TimestampType;
import dev.horizon.leadseal.Type;
import dev.horizon.leadseal.UuidType;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.attribute.TableAttribute;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContractTest {

  protected static final Type BOOLEAN = new BooleanType();
  protected static final Type INT = new IntegerType(IntegerType.Size.INT32);
  protected static final Type BIGINT = new IntegerType(IntegerType.Size.INT64);
  protected static final Type REAL = new FloatType(FloatType.Size.FLOAT32);
  protected static final Type DOUBLE = new FloatType(FloatType.Size.FLOAT64);
  protected static final Type DECIMAL = new DecimalType(12, 2);
  protected static final Type TEXT = new TextType(null);
  protected static final Type BYTES = new BytesType(null);
  protected static final Type UUID = new UuidType();
  protected static final Type TIMESTAMP = new TimestampType();
  protected static final Type DATE = new DateType();
  protected static final Type TIME = new TimeType();
  protected static final Type JSON = new JsonType();
  private static final AtomicInteger SCHEMAS = new AtomicInteger();

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
    new Migrations(migrations, List.of(dialect), MigrationOptions.defaults().withClock(Clock.fixed(now, ZoneOffset.UTC)))
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

  protected String q(String schema, String table) {
    return dialect.quote(schema) + "." + dialect.quote(table);
  }

  protected TestDatabase testDatabase() {
    return testDatabase;
  }

  protected Connection connection() {
    return connection;
  }

  protected String uniqueSchema() {
    return "s" + SCHEMAS.incrementAndGet();
  }

  protected long count(String sql, Object... params) throws SQLException {
    return Long.parseLong(query(sql, params).getFirst().getFirst());
  }

  protected int execute(String sql, Object... params) throws SQLException {
    try (PreparedStatement statement = prepare(sql, params)) {
      return statement.executeUpdate();
    }
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

  protected Map<String, Boolean> columns(String table) throws SQLException {
    Map<String, Boolean> columns = new LinkedHashMap<>();
    try (ResultSet result = metadata().getColumns(connection.getCatalog(), connection.getSchema(), table, null)) {
      while (result.next()) {
        columns.put(lower(result.getString("COLUMN_NAME")), "YES".equals(result.getString("IS_NULLABLE")));
      }
    }
    return columns;
  }

  protected List<String> primaryKey(String table) throws SQLException {
    Map<Integer, String> columns = new TreeMap<>();
    try (ResultSet result = metadata().getPrimaryKeys(connection.getCatalog(), connection.getSchema(), table)) {
      while (result.next()) {
        columns.put(result.getInt("KEY_SEQ"), lower(result.getString("COLUMN_NAME")));
      }
    }
    return List.copyOf(columns.values());
  }

  protected Map<String, Boolean> indexes(String table) throws SQLException {
    Map<String, Boolean> indexes = new TreeMap<>();
    try (ResultSet result = metadata().getIndexInfo(connection.getCatalog(), connection.getSchema(), table, false,
        false)) {
      while (result.next()) {
        String name = lower(result.getString("INDEX_NAME"));
        if (name != null && (name.startsWith("ix_") || name.startsWith("ux_"))) {
          indexes.put(name, !result.getBoolean("NON_UNIQUE"));
        }
      }
    }
    return indexes;
  }

  protected List<String> foreignKeys(String table) throws SQLException {
    TreeSet<String> keys = new TreeSet<>();
    try (ResultSet result = metadata().getImportedKeys(connection.getCatalog(), connection.getSchema(), table)) {
      while (result.next()) {
        keys.add(lower(result.getString("FKCOLUMN_NAME")) + " -> " + lower(result.getString("PKTABLE_NAME")) + "."
            + lower(result.getString("PKCOLUMN_NAME")));
      }
    }
    return List.copyOf(keys);
  }

  protected boolean tableExists(String table) throws SQLException {
    try (ResultSet result = metadata().getTables(connection.getCatalog(), connection.getSchema(), table,
        new String[] { "TABLE" })) {
      return result.next();
    }
  }

  protected static Column column(String id, String name, Type type) {
    return Column.of(id, name, type, NotNullAttribute.INSTANCE);
  }

  protected static Column nullable(String id, String name, Type type) {
    return Column.of(id, name, type);
  }

  protected static Column text(String id, String name, int length) {
    return column(id, name, new TextType(length));
  }

  protected static Table table(String id, String name, List<Column> columns, List<String> key,
      List<? extends TableAttribute> indexes, List<? extends TableAttribute> keys) {
    List<TableAttribute> all = new ArrayList<>();
    all.add(PrimaryKeyAttribute.of(name, key));
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

  static Stream<Type> types() {
    return Stream.of(BOOLEAN, INT, BIGINT, REAL, DOUBLE, DECIMAL, TEXT, BYTES, UUID, TIMESTAMP, DATE, TIME, JSON);
  }

  static Stream<Type> integers() {
    return Stream.of(INT, BIGINT);
  }

  static Stream<Type> dates() {
    return Stream.of(TIMESTAMP, DATE);
  }

  private PreparedStatement prepare(String sql, Object... params) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql);
    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
    return statement;
  }

  private DatabaseMetaData metadata() throws SQLException {
    return connection.getMetaData();
  }

  private static String lower(String name) {
    return name == null ? null : name.toLowerCase(Locale.ROOT);
  }
}
