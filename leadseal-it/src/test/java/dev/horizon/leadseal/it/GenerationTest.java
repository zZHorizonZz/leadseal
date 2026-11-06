package dev.horizon.leadseal.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.migration.MigrationDirectory;
import dev.horizon.leadseal.migration.MigrationOptions;
import dev.horizon.leadseal.type.IntegerType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerationTest {

  private static final IntegerType BIGINT = new IntegerType(IntegerType.Size.INT64);

  @TempDir
  Path migrations;

  private Schema orders(TableAttribute... indexes) {
    List<Column> columns = List.of(Column.of("1", "id", BIGINT, NotNull.INSTANCE), Column.of("2", "customer_id",
        BIGINT, NotNull.INSTANCE), Column.of("3", "line", BIGINT, NotNull.INSTANCE));
    Table table = Table.of("shop.Order", "orders", columns, PrimaryKey.of("orders", List.of("id")));
    for (TableAttribute index : indexes) {
      table = table.with(index);
    }
    return Schema.of(table);
  }

  private MigrationDirectory directory() {
    return new MigrationDirectory(migrations, Stream.of("postgresql", "sqlserver", "sqlite", "mariadb").map(Dialect::of)
        .toList(),
        MigrationOptions.defaults().withClock(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),
            ZoneOffset.UTC)));
  }

  private Map<Path, String> files() throws IOException {
    Map<Path, String> all = new TreeMap<>();
    try (Stream<Path> paths = Files.walk(migrations)) {
      for (Path file : paths.filter(Files::isRegularFile).toList()) {
        all.put(migrations.relativize(file), Files.readString(file));
      }
    }
    return all;
  }

  @Test
  @DisplayName("Writes no file for any database when one database refuses the first migration")
  void refuseFirst() throws IOException {
    Schema schema = orders(Index.of("orders", List.of("customer_id"), true).withWhere("line = 1"));

    assertThatThrownBy(() -> directory().generate(schema)).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("MariaDB has no partial indexes");
    assertThat(files()).isEmpty();
  }

  @Test
  @DisplayName("Keeps the files as they were when one database refuses a later migration")
  void refuseLater() throws IOException {
    directory().generate(orders());
    Map<Path, String> before = files();

    assertThatThrownBy(() -> directory().generate(orders(Index.of("orders", List.of("customer_id"), true).withWhere(
        "line = 1")))).isInstanceOf(UnsupportedChangeException.class).hasMessageContaining(
            "MariaDB has no partial indexes");
    assertThat(files()).isEqualTo(before);
    assertThat(before.keySet()).filteredOn(file -> file.toString().endsWith(".sql")).hasSize(4);
  }

  @Test
  @DisplayName("Writes the next migration for every database once the refused change is gone")
  void generateAfterRefusal() throws IOException {
    directory().generate(orders());
    assertThatThrownBy(() -> directory().generate(orders(Index.of("orders", List.of("customer_id"), true).withWhere(
        "line = 1")))).isInstanceOf(UnsupportedChangeException.class);

    directory().generate(orders(Index.of("orders", List.of("customer_id"), true)));

    assertThat(files().keySet()).filteredOn(file -> file.toString().endsWith(".sql")).hasSize(8);
  }
}
