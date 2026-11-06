package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.migration.MigrationDirectory;
import dev.horizon.leadseal.migration.MigrationFile;
import dev.horizon.leadseal.migration.MigrationOptions;
import dev.horizon.leadseal.migration.MigrationPlan;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DataLossTest {

  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNull.INSTANCE);
  private static final Column NOTE = Column.of("4", "note", TextType.unlimited());
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T07:00:00Z"), ZoneOffset.UTC);
  private static final EnumType.Value NEW = new EnumType.Value(0, "NEW");
  private static final EnumType.Value SENT = new EnumType.Value(1, "SENT");

  @TempDir
  Path directory;

  private static Table orders(Column... columns) {
    return Table.of("shop.Order", "orders", Stream.concat(Stream.of(ID), Stream.of(columns)).toList(),
        PrimaryKey.of("orders", List.of("id")));
  }

  private static Table customers() {
    return Table.of("shop.Customer", "customers", List.of(ID), PrimaryKey.of("customers", List.of("id")));
  }

  private MigrationDirectory migrations(MigrationOptions options) {
    return new MigrationDirectory(directory, List.of(new TestDialect("postgresql")), options.withClock(CLOCK));
  }

  private MigrationDirectory migrations() {
    return migrations(MigrationOptions.defaults());
  }

  private String script(MigrationPlan plan) {
    return plan.files().stream().filter(file -> file.path().toString().endsWith(".sql")).map(MigrationFile::content)
        .findFirst().orElseThrow();
  }

  @Test
  @DisplayName("Drops a column and warns about its values")
  void dropColumn() {
    migrations().generate(Schema.of(orders(NOTE)));

    MigrationPlan plan = migrations().generate(Schema.of(orders()));

    assertThat(plan.warnings()).containsExactly("drops the column orders.note and its values");
    assertThat(script(plan)).startsWith("-- Warning: drops the column orders.note and its values.\n\n");
  }

  @Test
  @DisplayName("Drops a table and warns about its rows")
  void dropTable() {
    migrations().generate(Schema.of(orders(), customers()));

    MigrationPlan plan = migrations().generate(Schema.of(orders()));

    assertThat(plan.warnings()).containsExactly("drops the table customers and its rows");
    assertThat(script(plan)).startsWith("-- Warning: drops the table customers and its rows.\n\n");
  }

  @Test
  @DisplayName("Drops the column of a renumbered field and adds a new one")
  void renumberField() {
    migrations().generate(Schema.of(orders(NOTE)));

    MigrationPlan plan = migrations().generate(Schema.of(orders(Column.of("5", "note", TextType.unlimited()))));

    assertThat(plan.warnings()).containsExactly("drops the column orders.note and its values");
  }

  @Test
  @DisplayName("Writes no warning for a migration that keeps every value")
  void keepValues() {
    migrations().generate(Schema.of(orders()));

    MigrationPlan plan = migrations().generate(Schema.of(orders(NOTE)));

    assertThat(plan.warnings()).isEmpty();
    assertThat(script(plan)).doesNotContain("--");
  }

  @Test
  @DisplayName("Applies an unsafe change and warns about it when the options allow unsafe changes")
  void allowUnsafe() {
    migrations().generate(Schema.of(orders(NOTE)));

    MigrationPlan plan = migrations(MigrationOptions.defaults().allowUnsafe())
        .generate(Schema.of(orders(Column.of("4", "note", TextType.of(10)))));

    assertThat(plan.warnings()).containsExactly("changes the column orders.note from text to text(10), which can lose"
        + " values");
  }

  static Stream<Arguments> lossyTypes() {
    return Stream.of(
        arguments(TextType.unlimited(), TextType.of(20)),
        arguments(TextType.of(40), TextType.of(20)),
        arguments(new IntegerType(IntegerType.Size.INT64), new IntegerType(IntegerType.Size.INT32)),
        arguments(new FloatType(FloatType.Size.FLOAT64), new FloatType(FloatType.Size.FLOAT32)),
        arguments(new DecimalType(12, 2), new DecimalType(12, 1)),
        arguments(new DecimalType(12, 2), new DecimalType(10, 2)),
        arguments(TextType.unlimited(), new IntegerType(IntegerType.Size.INT64)),
        arguments(names(NEW, SENT), names(NEW)),
        arguments(names(NEW, SENT), TextType.of(3)));
  }

  @ParameterizedTest(name = "({0} to {1})")
  @MethodSource("lossyTypes")
  @DisplayName("Refuses a type change that can lose values")
  void refuseLossyType(Type origin, Type target) {
    migrations().generate(Schema.of(orders(Column.of("4", "note", origin))));

    assertThatThrownBy(() -> migrations().generate(Schema.of(orders(Column.of("4", "note", target)))))
        .isInstanceOf(UnsupportedChangeException.class).hasMessageContaining("which can lose data and is unsafe");
  }

  static Stream<Arguments> safeTypes() {
    return Stream.of(
        arguments(TextType.of(20), TextType.unlimited()),
        arguments(TextType.of(20), TextType.of(40)),
        arguments(new IntegerType(IntegerType.Size.INT32), new IntegerType(IntegerType.Size.INT64)),
        arguments(new FloatType(FloatType.Size.FLOAT32), new FloatType(FloatType.Size.FLOAT64)),
        arguments(new DecimalType(10, 2), new DecimalType(12, 3)),
        arguments(new UuidType(), TextType.unlimited()),
        arguments(names(NEW), names(NEW, SENT)),
        arguments(names(NEW, SENT), TextType.of(4)),
        arguments(new EnumType("state", List.of(NEW, SENT), EnumType.Storage.NUMBER),
            new IntegerType(IntegerType.Size.INT32)));
  }

  @ParameterizedTest(name = "({0} to {1})")
  @MethodSource("safeTypes")
  @DisplayName("Changes a type that holds every value of the earlier type, without a warning")
  void changeSafeType(Type origin, Type target) {
    migrations().generate(Schema.of(orders(Column.of("4", "note", origin))));

    MigrationPlan plan = migrations().generate(Schema.of(orders(Column.of("4", "note", target))));

    assertThat(plan.migration()).isPresent();
    assertThat(plan.warnings()).isEmpty();
  }

  private static EnumType names(EnumType.Value... values) {
    return new EnumType("state", List.of(values), EnumType.Storage.NAME);
  }
}
