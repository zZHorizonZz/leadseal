package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Type;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefaultAttribute;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class DefaultContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);

  static Stream<Arguments> values() {
    return Stream.of(
        arguments(BOOLEAN, "true"),
        arguments(INT, "42"),
        arguments(BIGINT, "5000000000"),
        arguments(REAL, "1.5"),
        arguments(DOUBLE, "2.25"),
        arguments(DECIMAL, "12.50"),
        arguments(TEXT, "it's a \\ test"),
        arguments(BYTES, "0aff"),
        arguments(UUID, "123e4567-e89b-12d3-a456-426614174000"),
        arguments(TIMESTAMP, "2026-10-14T07:30:00Z"),
        arguments(DATE, "2026-10-14"),
        arguments(TIME, "07:30:00"),
        arguments(JSON, "{\"a\": 1}"));
  }

  private static Table orders(Column... columns) {
    return table("shop.Order", "orders", List.of(columns), "id");
  }

  private long matching(Column column, String value) throws SQLException {
    return count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q(column.name()) + " = "
        + dialect.literal(column.type(), value));
  }

  @ParameterizedTest
  @MethodSource("values")
  void fillsNewRowsWithTheDefault(Type type, String value) throws SQLException {
    Column column = column("2", "value", type).with(new LiteralDefaultAttribute(value));
    apply(schema(orders(ID, column)));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    assertThat(matching(column, value)).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("values")
  void fillsExistingRowsWithTheDefaultOfANewColumn(Type type, String value) throws SQLException {
    apply(schema(orders(ID)));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");
    Column column = column("2", "value", type).with(new LiteralDefaultAttribute(value));

    apply(schema(orders(ID, column)));

    assertThat(matching(column, value)).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("dates")
  void generatesTheCurrentTime(Type type) throws SQLException {
    apply(schema(orders(ID)));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, column("2", "created", type).with(new NowDefaultAttribute()))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (2)");

    assertThat(count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("created") + " IS NOT NULL"))
        .isEqualTo(2);
  }

  @Test
  void generatesRandomUuids() throws SQLException {
    apply(schema(orders(ID)));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, column("2", "token", UUID).with(new RandomUuidDefaultAttribute()))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (2)");
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (3)");

    assertThat(count("SELECT COUNT(DISTINCT " + q("token") + ") FROM " + q("orders"))).isEqualTo(3);
  }

  @Test
  void changesTheDefault() throws SQLException {
    Column total = column("2", "total", INT);
    apply(schema(orders(ID, total.with(new LiteralDefaultAttribute("1")))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, total.with(new LiteralDefaultAttribute("2")))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (2)");

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders") + " ORDER BY " + q("id")))
        .containsExactly(List.of("1"), List.of("2"));
  }

  @Test
  void removesTheDefault() throws SQLException {
    Column total = column("2", "total", INT);
    apply(schema(orders(ID, total.with(new LiteralDefaultAttribute("1")))));

    apply(schema(orders(ID, total)));

    assertThatThrownBy(() -> execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void fillsNullsWithTheDefaultWhenAColumnBecomesRequired() throws SQLException {
    apply(schema(orders(ID, nullable("2", "state", TEXT))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

    apply(schema(orders(ID, column("2", "state", TEXT).with(new LiteralDefaultAttribute("new")))));

    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }
}
