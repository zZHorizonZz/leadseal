package dev.horizon.leadseal;

import static dev.horizon.leadseal.Fixtures.column;
import static dev.horizon.leadseal.Fixtures.nullable;
import static dev.horizon.leadseal.Fixtures.schema;
import static dev.horizon.leadseal.Fixtures.table;
import static dev.horizon.leadseal.Fixtures.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationsTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Schema FIRST = schema(table("shop.Order", "orders").column(ID).column(text("2", "state", 20))
      .primaryKey("id").build());
  private static final Schema SECOND = schema(table("shop.Order", "orders").column(ID).column(text("2", "state", 20))
      .column(nullable("3", "note", ColumnType.TEXT)).primaryKey("id").build());
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T07:00:00Z"), ZoneOffset.UTC);

  @TempDir
  Path directory;

  @Test
  void writesTheFirstMigration() throws IOException {
    Migrations.Result result = migrations("postgresql", "sqlite").generate(FIRST);

    Path folder = directory.resolve("20261014T070000_create_orders");
    assertThat(result.migration()).contains(Path.of("20261014T070000_create_orders"));
    assertThat(folder.resolve("migration.json")).exists();
    assertThat(folder.resolve("ops.json")).content().contains("\"op\" : \"create_table\"");
    assertThat(folder.resolve("postgresql/V20261014070000__create_orders.sql")).content()
        .startsWith("CREATE TABLE \"orders\"");
    assertThat(folder.resolve("sqlite/V20261014070000__create_orders.sql")).exists();
    assertThat(directory.resolve("snapshots").resolve(FIRST.hash()).resolve("schema.json")).exists();
    assertThat(Files.readString(folder.resolve("migration.json"))).contains("\"to\" : \"" + FIRST.hash() + "\"")
        .doesNotContain("\"from\"");
  }

  @Test
  void plansWithoutWriting() throws IOException {
    Migrations.Result result = migrations("postgresql").plan(FIRST);

    assertThat(result.files()).extracting(file -> file.path().toString().replace('\\', '/')).containsExactly(
        "snapshots/" + FIRST.hash() + "/schema.json", "20261014T070000_create_orders/ops.json",
        "20261014T070000_create_orders/migration.json",
        "20261014T070000_create_orders/postgresql/V20261014070000__create_orders.sql");
    try (Stream<Path> files = Files.list(directory)) {
      assertThat(files).isEmpty();
    }
  }

  @Test
  void writesNothingWhenTheSchemaDidNotChange() {
    migrations("postgresql").generate(FIRST);

    Migrations.Result result = migrations("postgresql").generate(FIRST);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).isEmpty();
  }

  @Test
  void continuesFromTheLatestSnapshot() throws IOException {
    migrations("postgresql").generate(FIRST);

    Path second = directory.resolve(migrations("postgresql").generate(SECOND).migration().orElseThrow());

    assertThat(second.getFileName().toString()).isEqualTo("20261014T070001_add_orders_note");
    assertThat(second.resolve("postgresql/V20261014070001__add_orders_note.sql")).content()
        .isEqualTo("ALTER TABLE \"orders\" ADD COLUMN \"note\" TEXT;\n");
    assertThat(Files.readString(second.resolve("migration.json"))).contains("\"from\" : \"" + FIRST.hash() + "\"");
  }

  @Test
  void neverRewritesAnExistingMigration() throws IOException {
    Path first = directory.resolve(migrations("postgresql").generate(FIRST).migration().orElseThrow());
    Path file = first.resolve("postgresql/V20261014070000__create_orders.sql");
    Files.writeString(file, "-- edited by hand\n");

    migrations("postgresql").generate(SECOND);

    assertThat(file).content().isEqualTo("-- edited by hand\n");
  }

  @Test
  void readsEveryOptionBackFromItsFiles() {
    Column state = new Column("2", "state", ColumnType.TEXT, 20, null, null, false, Default.value(""), false,
        Map.of("postgresql", "C"), "The state.");
    Column price = new Column("3", "price", ColumnType.DECIMAL, null, 12, 2, true, null, false, null, null);
    Column created = new Column("4", "created", ColumnType.TIMESTAMP, null, false).withDefault(Default.now());
    Schema schema = schema(new Table("shop.Order", "orders", "sales", List.of(ID.withIdentity(true), state, price,
        created), PrimaryKey.of("orders", List.of("id")),
        List.of(Index.of("orders", List.of("state", "created"), true)
            .withDescending(List.of("created")).withWhere("price IS NOT NULL")),
        List.of(), List.of(Check.of(
            "orders", "price >= 0")),
        "The orders."));
    migrations("postgresql").generate(schema);

    Migrations.Result result = migrations("postgresql", "mariadb").generate(schema);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).singleElement().satisfies(file -> assertThat(file.path().toString()).contains(
        "mariadb"));
  }

  @Test
  void addsTheFilesOfANewDialectToEveryMigration() {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);

    Migrations.Result result = migrations("postgresql", "mariadb").generate(SECOND);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).hasSize(2).allSatisfy(file -> assertThat(file.path().toString()).contains("mariadb"));
    assertThat(directory.resolve("20261014T070001_add_orders_note/mariadb/V20261014070001__add_orders_note.sql"))
        .exists();
  }

  @Test
  void rejectsTwoBranches() throws IOException {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);
    Path second = directory.resolve("20261014T070001_add_orders_note");
    Path copy = directory.resolve("20261014T070002_add_orders_note");
    Files.createDirectories(copy);
    Files.copy(second.resolve("migration.json"), copy.resolve("migration.json"));

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(SchemaException.class)
        .hasMessageContaining("both start from the same schema");
  }

  @Test
  void rejectsAMissingParent() throws IOException {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);
    try (Stream<Path> files = Files.walk(directory.resolve("20261014T070000_create_orders"))) {
      files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
    }

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(SchemaException.class)
        .hasMessageContaining("which no other migration creates");
  }

  private Migrations migrations(String... dialects) {
    return new Migrations(directory, Stream.of(dialects).<Dialect> map(StandardDialect::new).toList(), CLOCK);
  }

  @Test
  void needsADialect() {
    assertThatThrownBy(() -> new Migrations(directory, List.of())).isInstanceOf(SchemaException.class);
  }
}
