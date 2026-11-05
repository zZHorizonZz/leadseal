package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.NowDefault;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.migration.MigrationDirectory;
import dev.horizon.leadseal.migration.MigrationOptions;
import dev.horizon.leadseal.migration.MigrationPlan;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimestampType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationsTest {

  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64),
      NotNull.INSTANCE);
  private static final Column STATE = Column.of("2", "state", TextType.of(20), NotNull.INSTANCE);
  private static final Schema FIRST = Schema.of(orders(ID, STATE));
  private static final Schema SECOND = Schema.of(orders(ID, STATE, Column.of("3", "note", TextType.unlimited())));
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T07:00:00Z"), ZoneOffset.UTC);

  @TempDir
  Path directory;

  private static Table orders(Column... columns) {
    return Table.of("shop.Order", "orders", List.of(columns), PrimaryKey.of("orders", List.of("id")));
  }

  private String hash(Schema schema) {
    return new MigrationDirectory(directory.resolve("empty"), List.of(new TestDialect("postgresql"))).plan(schema).files()
        .getFirst().path().getName(1).toString();
  }

  private MigrationDirectory migrations(String... dialects) {
    return new MigrationDirectory(directory, Stream.of(dialects).<Dialect> map(TestDialect::new).toList(),
        MigrationOptions.defaults().withClock(CLOCK));
  }

  @Test
  @DisplayName("Writes the first migration")
  void writeFirstMigration() throws IOException {
    MigrationPlan result = migrations("postgresql", "sqlite").generate(FIRST);

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
  @DisplayName("Hashes the same schema the same")
  void hashStably() {
    assertThat(hash(FIRST)).isEqualTo(hash(Schema.of(orders(ID, STATE)))).hasSize(16);
    assertThat(hash(FIRST)).isNotEqualTo(hash(SECOND));
  }

  @Test
  @DisplayName("Plans without writing")
  void planWithoutWriting() throws IOException {
    MigrationPlan result = migrations("postgresql").plan(FIRST);

    assertThat(result.files()).extracting(file -> file.path().toString().replace('\\', '/')).containsExactly(
        "snapshots/" + hash(FIRST) + "/contract.json", "20261014070000/migration.json",
        "20261014070000/postgresql/V20261014070000.sql");
    try (Stream<Path> files = Files.list(directory)) {
      assertThat(files).isEmpty();
    }
  }

  @Test
  @DisplayName("Writes nothing when the schema did not change")
  void writeNothingUnchanged() {
    migrations("postgresql").generate(FIRST);

    MigrationPlan result = migrations("postgresql").generate(FIRST);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).isEmpty();
  }

  @Test
  @DisplayName("Continues from the latest snapshot")
  void continueFromSnapshot() throws IOException {
    migrations("postgresql").generate(FIRST);

    Path second = directory.resolve(migrations("postgresql").generate(SECOND).migration().orElseThrow());

    assertThat(second.getFileName().toString()).isEqualTo("20261014070001");
    assertThat(second.resolve("postgresql/V20261014070001.sql")).content()
        .isEqualTo("ALTER TABLE \"orders\" ADD COLUMN \"note\" TEXT;\n");
    assertThat(Files.readString(second.resolve("migration.json"))).contains("\"from\" : \"" + hash(FIRST) + "\"");
  }

  @Test
  @DisplayName("Never rewrites an existing migration")
  void keepExistingMigration() throws IOException {
    Path first = directory.resolve(migrations("postgresql").generate(FIRST).migration().orElseThrow());
    Path file = first.resolve("postgresql/V20261014070000.sql");
    Files.writeString(file, "-- edited by hand\n");

    migrations("postgresql").generate(SECOND);

    assertThat(file).content().isEqualTo("-- edited by hand\n");
  }

  @Test
  @DisplayName("Reads every attribute back from its files")
  void readAttributesBack() {
    Column state = Column.of("2", "state", TextType.of(20), NotNull.INSTANCE,
        new LiteralDefault(""), new Collation("postgresql", "C"),
        new Comment("State."));
    Column price = Column.of("3", "price", new DecimalType(12, 2));
    Column created = Column.of("4", "created", new TimestampType(), NotNull.INSTANCE,
        new NowDefault());
    Table orders = Table.of("shop.Order", "orders", List.of(ID.with(Identity.INSTANCE), state, price,
        created), PrimaryKey.of("orders", List.of("id")),
        Index.of("orders", List.of("state", "created"), true).withDescending(List.of("created"))
            .withWhere("price IS NOT NULL"),
        Check.of("orders", "price >= 0"), new Comment("The orders."));
    Schema schema = Schema.of(new Namespace("sales", List.of(orders)));
    migrations("postgresql").generate(schema);

    MigrationPlan result = migrations("postgresql", "mariadb").generate(schema);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).singleElement()
        .satisfies(file -> assertThat(file.path().toString()).contains("mariadb"));
  }

  @Test
  @DisplayName("Adds the files of a new dialect to every migration")
  void addNewDialect() {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);

    MigrationPlan result = migrations("postgresql", "mariadb").generate(SECOND);

    assertThat(result.migration()).isEmpty();
    assertThat(result.files()).hasSize(2).allSatisfy(file -> assertThat(file.path().toString()).contains("mariadb"));
    assertThat(directory.resolve("20261014070001/mariadb/V20261014070001.sql"))
        .exists();
  }

  @Test
  @DisplayName("Rejects two branches")
  void rejectBranches() throws IOException {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);
    Path second = directory.resolve("20261014070001");
    Path copy = Files.createDirectories(directory.resolve("20261014070002"));
    Files.writeString(copy.resolve("migration.json"), Files.readString(second.resolve("migration.json"))
        .replace("20261014070001", "20261014070002"));

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(BrokenHistoryException.class)
        .hasMessageContaining("but the migration before it, 20261014070001, creates the schema");
  }

  @Test
  @DisplayName("Rejects a missing parent")
  void rejectMissingParent() throws IOException {
    migrations("postgresql").generate(FIRST);
    migrations("postgresql").generate(SECOND);
    try (Stream<Path> files = Files.walk(directory.resolve("20261014070000"))) {
      files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
    }

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(BrokenHistoryException.class)
        .hasMessageContaining("which no migration before it creates");
  }

  @Test
  @DisplayName("Rejects a folder that does not match its migration")
  void rejectMismatchedFolder() throws IOException {
    Path first = directory.resolve(migrations("postgresql").generate(FIRST).migration().orElseThrow());
    Files.move(first, directory.resolve("20261014070000_orders"));

    assertThatThrownBy(() -> migrations("postgresql").generate(SECOND)).isInstanceOf(BrokenHistoryException.class)
        .hasMessageContaining("Rename the folder to 20261014070000");
  }

  @Test
  @DisplayName("Needs a dialect")
  void needDialect() {
    assertThatThrownBy(() -> new MigrationDirectory(directory, List.of())).isInstanceOf(ConfigurationException.class);
  }

  @Test
  @DisplayName("Plans nothing for the latest schema without its snapshot")
  void planUpToDate() throws IOException {
    migrations("postgresql").generate(FIRST);
    try (Stream<Path> files = Files.walk(directory.resolve("snapshots"))) {
      files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
    }

    MigrationPlan plan = migrations("postgresql").plan(FIRST);

    assertThat(plan.migration()).isEmpty();
    assertThat(plan.files()).isEmpty();
  }
}
