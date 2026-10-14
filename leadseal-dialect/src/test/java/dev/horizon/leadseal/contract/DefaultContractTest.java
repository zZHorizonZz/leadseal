package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class DefaultContractTest extends ContractTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);

  static Stream<Arguments> values() {
    return Stream.of(
        arguments(ColumnType.BOOLEAN, "true"),
        arguments(ColumnType.INT, "42"),
        arguments(ColumnType.BIGINT, "5000000000"),
        arguments(ColumnType.REAL, "1.5"),
        arguments(ColumnType.DOUBLE, "2.25"),
        arguments(ColumnType.DECIMAL, "12.50"),
        arguments(ColumnType.TEXT, "it's a \\ test"),
        arguments(ColumnType.BYTES, "0aff"),
        arguments(ColumnType.UUID, "123e4567-e89b-12d3-a456-426614174000"),
        arguments(ColumnType.TIMESTAMP, "2026-10-14T07:30:00Z"),
        arguments(ColumnType.DATE, "2026-10-14"),
        arguments(ColumnType.TIME, "07:30:00"),
        arguments(ColumnType.JSON, "{\"a\": 1}"));
  }

  private static Table orders(Column... columns) {
    return table("shop.Order", "orders", List.of(columns), "id");
  }

  private long matching(Column column, String value) throws SQLException {
    return count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q(column.name()) + " = "
        + dialect.literal(column, value));
  }

  @ParameterizedTest
  @MethodSource("values")
  void fillsNewRowsWithTheDefault(ColumnType type, String value) throws SQLException {
    Column column = column("2", "value", type).withDefault(Default.value(value));
    apply(schema(orders(ID, column)));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    assertThat(matching(column, value)).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("values")
  void fillsExistingRowsWithTheDefaultOfANewColumn(ColumnType type, String value) throws SQLException {
    apply(schema(orders(ID)));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");
    Column column = column("2", "value", type).withDefault(Default.value(value));

    apply(schema(orders(ID, column)));

    assertThat(matching(column, value)).isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(value = ColumnType.class, names = { "TIMESTAMP", "DATE" })
  void generatesTheCurrentTime(ColumnType type) throws SQLException {
    apply(schema(orders(ID)));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, column("2", "created", type).withDefault(Default.now()))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (2)");

    assertThat(count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("created") + " IS NOT NULL"))
        .isEqualTo(2);
  }

  @Test
  void generatesRandomUuids() throws SQLException {
    apply(schema(orders(ID)));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, column("2", "token", ColumnType.UUID).withDefault(Default.randomUuid()))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (2)");
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (3)");

    assertThat(count("SELECT COUNT(DISTINCT " + q("token") + ") FROM " + q("orders"))).isEqualTo(3);
  }

  @Test
  void changesTheDefault() throws SQLException {
    Column total = column("2", "total", ColumnType.INT);
    apply(schema(orders(ID, total.withDefault(Default.value("1")))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, total.withDefault(Default.value("2")))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (2)");

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders") + " ORDER BY " + q("id")))
        .containsExactly(List.of("1"), List.of("2"));
  }

  @Test
  void removesTheDefault() throws SQLException {
    Column total = column("2", "total", ColumnType.INT);
    apply(schema(orders(ID, total.withDefault(Default.value("1")))));

    apply(schema(orders(ID, total)));

    assertThatThrownBy(() -> execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void fillsNullsWithTheDefaultWhenAColumnBecomesRequired() throws SQLException {
    apply(schema(orders(ID, nullable("2", "state", ColumnType.TEXT))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, column("2", "state", ColumnType.TEXT).withDefault(Default.value("new")))));

    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }
}
