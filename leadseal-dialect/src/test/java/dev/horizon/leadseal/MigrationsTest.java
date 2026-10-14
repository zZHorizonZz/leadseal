package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationsTest {

  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64),
      NotNullAttribute.INSTANCE);
  private static final Column STATE = Column.of("2", "state", new TextType(20), NotNullAttribute.INSTANCE);
  private static final Schema FIRST = Schema.of(orders(ID, STATE));
  private static final Schema SECOND = Schema.of(orders(ID, STATE, Column.of("3", "note", new TextType(null))));
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T07:00:00Z"), ZoneOffset.UTC);

  @TempDir
  Path directory;

  private static Table orders(Column... columns) {
    return Table.of("shop.Order", "orders", List.of(columns), PrimaryKeyAttribute.of("orders", List.of("id")));
  }

  private String hash(Schema schema) {
    return new Migrations(directory.resolve("empty"), List.of(new StandardDialect("postgresql"))).plan(schema).files()
        .getFirst().path().getName(1).toString();
  }

  private Migrations migrations(String... dialects) {
    return new Migrations(directory, Stream.of(dialects).<Dialect> map(StandardDialect::new).toList(),
        MigrationOptions.defaults().withClock(CLOCK));
  }

  @Test
  void writesTheFirstMigration() throws IOException {
    Migrations.Result result = migrations("postgresql", "sqlite").generate(FIRST);

    Path folder = directory.resolve("20261014070000");
    assertThat(result.migration()).contains(Path.of("20261014070000"));
    assertThat(folder.resolve("migration.json")).content().contains("\"version\" : \"20261014070000\"")
        .contains("\"change\" : \"create_table\"");
    assertThat(folder.resolve("postgresql/V20261014070000.sql")).content()
        .startsWith("CREATE TABLE \"orders\"");
    assertThat(folder.resolve("sqlite/V20261014070000.sql")).exists();
    assertThat(directory.resolve("snapshots").resolve(hash(FIRST)).resolve("contract.json")).content()
        .contains("\"schemaVersion\" : \"1\"").contains("\"storageHash\" : \"" + hash(FIRST) + "\"");
    assertThat(Files.readString(folder.resolve("migration.json"))).contains("\"to\" : \"" + hash(FIRST) + "\"")
        .doesNotContain("\"from\"");
  }

  @Test
  void hashesTheSameSchemaTheSame() {
    assertThat(hash(FIRST)).isEqualTo(hash(Schema.of(orders(ID, STATE)))).hasSize(16);
    assertThat(hash(FIRST)).isNotEqualTo(hash(SECOND));
  }

  @Test
  void plansWithoutWriting() throws IOException {
    Migrations.Result result = migrations("postgresql").plan(FIRST);

    assertThat(result.files()).extracting(file -> file.path().toString().replace('\\', '/')).containsExactly(
        "snapshots/" + hash(FIRST) + "/contract.json", "20261014070000/migration.json",
        "20261014070000/postgresql/V20261014070000.sql");
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

    assertThat(second.getFileName().toString()).isEqualTo("20261014070001");
    assertThat(second.resolve("postgresql/V20261014070001.sql")).content()
        .isEqualTo("ALTER TABLE \"orders\" ADD COLUMN \"note\" TEXT;\n");
    assertThat(Files.readString(second.resolve("migration.json"))).contains("\"from\" : \"" + hash(FIRST) + "\"");
  }

  @Test
  void neverRewritesAnExistingMigration() throws IOException {
    Path first = directory.resolve(migrations("postgresql").generate(FIRST).migration().orElseThrow());
    Path file = first.resolve("postgresql/V20261014070000.sql");
    Files.writeString(file, "-- edited by hand\n");

    migrations("postgresql").generate(SECOND);

    assertThat(file).content().isEqualTo("-- edited by hand\n");
  }

  @Test
  void readsEveryAttributeBackFromItsFiles() {
    Column state = Column.of("2", "state", new TextType(20), NotNullAttribute.INSTANCE,
        new LiteralDefaultAttribute(""), new CollationAttribute("postgresql", "C"), new CommentAttribute("State."));
    Column price = Column.of("3", "price", new DecimalType(12, 2));
    Column created = Column.of("4", "created", new TimestampType(), NotNullAttribute.INSTANCE,
        new NowDefaultAttribute());
    Table orders = Table.of("shop.Order", "orders", List.of(ID.with(IdentityAttribute.INSTANCE), state, price,
        created), PrimaryKeyAttribute.of("orders", List.of("id")),
        IndexAttribute.of("orders", List.of("state", "created"), true).withDescending(List.of("created"))
            .withWhere("price IS NOT NULL"),
        CheckAttribute.of("orders", "price >= 0"), new CommentAttribute("The orders."));
    Schema schema = Schema.of(new Namespace("sales", List.of(orders)));
    migrations("postgresql").generate(schema);

    Migrations.Result result = migrations("postgresql", "mariadb").generate(schema);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).singleElement()
        .satisfies(file -> assertThat(file.path().toString()).contains("mariadb"));
  }

  @Test
  void addsTheFilesOfANewDialectToEveryMigration() {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);

    Migrations.Result result = migrations("postgresql", "mariadb").generate(SECOND);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).hasSize(2).allSatisfy(file -> assertThat(file.path().toString()).contains("mariadb"));
    assertThat(directory.resolve("20261014070001/mariadb/V20261014070001.sql"))
        .exists();
  }

  @Test
  void rejectsTwoBranches() throws IOException {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);
    Path second = directory.resolve("20261014070001");
    Path copy = Files.createDirectories(directory.resolve("20261014070002"));
    Files.writeString(copy.resolve("migration.json"), Files.readString(second.resolve("migration.json"))
        .replace("20261014070001", "20261014070002"));

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(SchemaException.class)
        .hasMessageContaining("but the migration before it, 20261014070001, creates the schema");
  }

  @Test
  void rejectsAMissingParent() throws IOException {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);
    try (Stream<Path> files = Files.walk(directory.resolve("20261014070000"))) {
      files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
    }

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(SchemaException.class)
        .hasMessageContaining("which no migration before it creates");
  }

  @Test
  void rejectsAFolderThatDoesNotMatchItsMigration() throws IOException {
    Path first = directory.resolve(migrations("postgresql").generate(FIRST).migration().orElseThrow());
    Files.move(first, directory.resolve("20261014070000_orders"));

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(SchemaException.class)
        .hasMessageContaining("Rename the folder to 20261014070000");
  }

  @Test
  void needsADialect() {
    assertThatThrownBy(() -> new Migrations(directory, List.of())).isInstanceOf(SchemaException.class);
  }
}
