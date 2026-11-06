package dev.horizon.leadseal.it;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.migration.MigrationDirectory;
import dev.horizon.leadseal.migration.MigrationOptions;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;

final class Session implements AutoCloseable {

  private static final Pattern LITERAL = Pattern.compile("literal\\((\\w+),\\s*'((?:[^']|'')*)'\\)");
  private static final Pattern CATALOG = Pattern.compile("(columns|primary key|indexes|foreign keys|comment)\\((.*)\\)");
  private static final Map<String, Type> TYPES = Map.ofEntries(Map.entry("bool", new BooleanType()),
      Map.entry("int32", new IntegerType(IntegerType.Size.INT32)),
      Map.entry("int64", new IntegerType(IntegerType.Size.INT64)), Map.entry("float", new FloatType(
          FloatType.Size.FLOAT32)),
      Map.entry("double", new FloatType(FloatType.Size.FLOAT64)), Map.entry("decimal", new DecimalType(12, 2)),
      Map.entry("string", TextType.unlimited()), Map.entry("bytes", BytesType.unlimited()), Map.entry("uuid",
          new UuidType()),
      Map.entry("timestamp", new TimestampType()), Map.entry("date", new DateType()), Map.entry("time",
          new TimeType()),
      Map.entry("json", new JsonType()));

  private final Database database;
  private final Dialect dialect;
  private final Cases cases;
  private final String suite;
  private final Database.Location location;
  private final Path migrations;
  private final Set<String> namespaces = new LinkedHashSet<>();
  private Connection connection;

  Session(Database database, Cases cases, String suite) throws SQLException, IOException {
    this.database = database;
    this.dialect = Dialect.of(database.dialect());
    this.cases = cases;
    this.suite = suite;
    this.location = database.create();
    this.connection = location.connect();
    this.migrations = Files.createTempDirectory("leadseal-behavior");
  }

  String dialect() {
    return database.dialect();
  }

  void migrate(String version, boolean unsafe) throws SQLException {
    Schema schema = cases.schema(suite, version);
    schema.namespaces().forEach(namespace -> namespace.name().map(namespaces::add, () -> false));
    MigrationOptions options = MigrationOptions.defaults().withClock(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),
        ZoneOffset.UTC));
    new MigrationDirectory(migrations, List.of(dialect), unsafe ? options.allowUnsafe() : options).generate(schema);
    Flyway.configure().dataSource(location.url(), location.user(), location.password())
        .locations("filesystem:" + migrations.toAbsolutePath().toString().replace('\\', '/') + "/**/"
            + database.dialect())
        .load().migrate();
    connection.close();
    connection = location.connect();
  }

  void execute(String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(render(sql));
    }
  }

  List<List<String>> query(String sql) throws SQLException {
    Matcher catalog = CATALOG.matcher(sql);
    if (catalog.matches()) {
      return catalog(catalog.group(1), catalog.group(2).strip());
    }
    List<List<String>> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(render(sql))) {
      int columns = result.getMetaData().getColumnCount();
      while (result.next()) {
        List<String> row = new ArrayList<>();
        for (int i = 1; i <= columns; i++) {
          row.add(switch (result.getObject(i)) {
            case Boolean value -> value ? "1" : "0";
            case byte[] bytes -> HexFormat.of().formatHex(bytes);
            case null -> null;
            default -> result.getString(i);
          });
        }
        rows.add(row);
      }
    }
    return rows;
  }

  private List<List<String>> catalog(String kind, String target) throws SQLException {
    DatabaseMetaData metadata = connection.getMetaData();
    List<List<String>> rows = new ArrayList<>();
    String[] parts = target.split("\\s*,\\s*");
    Database.Place place = database.place(connection, parts[0]);
    switch (kind) {
      case "columns" -> {
        try (ResultSet result = metadata.getColumns(place.catalog(), place.schema(), place.table(), null)) {
          while (result.next()) {
            rows.add(List.of(lower(result.getString("COLUMN_NAME")), "YES".equals(result.getString("IS_NULLABLE"))
                ? "optional"
                : "required"));
          }
        }
      }
      case "primary key" -> {
        Map<Integer, String> columns = new TreeMap<>();
        try (ResultSet result = metadata.getPrimaryKeys(place.catalog(), place.schema(), place.table())) {
          while (result.next()) {
            columns.put(result.getInt("KEY_SEQ"), lower(result.getString("COLUMN_NAME")));
          }
        }
        columns.values().forEach(column -> rows.add(List.of(column)));
      }
      case "indexes" -> {
        Map<String, String> indexes = new TreeMap<>();
        try (ResultSet result = metadata.getIndexInfo(place.catalog(), place.schema(), place.table(), false, false)) {
          while (result.next()) {
            String name = lower(result.getString("INDEX_NAME"));
            if (name != null && (name.startsWith("ix_") || name.startsWith("ux_"))) {
              indexes.put(name, result.getBoolean("NON_UNIQUE") ? "index" : "unique");
            }
          }
        }
        indexes.forEach((name, unique) -> rows.add(List.of(name, unique)));
      }
      case "foreign keys" -> {
        Set<String> keys = new TreeSet<>();
        try (ResultSet result = metadata.getImportedKeys(place.catalog(), place.schema(), place.table())) {
          while (result.next()) {
            keys.add(lower(result.getString("FKCOLUMN_NAME")) + " -> " + lower(result.getString("PKTABLE_NAME")) + "."
                + lower(result.getString("PKCOLUMN_NAME")));
          }
        }
        keys.forEach(key -> rows.add(List.of(key)));
      }
      default -> {
        String comment = database.comment(connection, place, parts.length > 1 ? parts[1] : null);
        if (comment != null) {
          rows.add(List.of(comment));
        }
      }
    }
    return rows;
  }

  private String render(String sql) {
    return LITERAL.matcher(sql).replaceAll(literal -> Matcher.quoteReplacement(dialect.literal(TYPES.get(literal
        .group(1)), literal.group(2).replace("''", "'"))));
  }

  private String lower(String name) {
    return name == null ? null : name.toLowerCase(Locale.ROOT);
  }

  @Override
  public void close() throws SQLException {
    for (String namespace : namespaces) {
      database.dropNamespace(connection, namespace);
    }
    connection.close();
  }
}
