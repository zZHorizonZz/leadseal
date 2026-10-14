package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Default;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class ColumnContractTest extends ContractTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Column STATE = text("2", "state", 20);

  private static Table orders(Column... columns) {
    return table("shop.Order", "orders", List.of(columns), "id");
  }

  private void insertOrder() throws SQLException {
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("state") + ") VALUES (1, 'new')");
  }

  @ParameterizedTest
  @EnumSource(ColumnType.class)
  void createsAColumnOfEachType(ColumnType type) throws SQLException {
    apply(schema(orders(ID, column("2", "value", type), nullable("3", "optional", type))));

    assertThat(columns("orders")).containsExactly(Map.entry("id", false), Map.entry("value", false),
        Map.entry("optional", true));
  }

  @ParameterizedTest
  @EnumSource(ColumnType.class)
  void addsARequiredColumnToATableWithRows(ColumnType type) throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, STATE, column("3", "value", type))));

    assertThat(columns("orders")).containsEntry("value", false);
    assertThat(query("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("value") + " IS NOT NULL"))
        .containsExactly(List.of("1"));
  }

  @Test
  void addsAnOptionalColumn() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, STATE, nullable("3", "note", ColumnType.TEXT))));

    assertThat(query("SELECT " + q("state") + ", " + q("note") + " FROM " + q("orders")))
        .containsExactly(Arrays.asList("new", null));
  }

  @Test
  void renamesAColumnAndKeepsItsValues() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, text("2", "status", 20))));

    assertThat(columns("orders")).containsOnlyKeys("id", "status");
    assertThat(query("SELECT " + q("status") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }

  @Test
  void widensATextColumn() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, text("2", "state", 200))));
    execute("UPDATE " + q("orders") + " SET " + q("state") + " = ?", "x".repeat(200));

    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("x".repeat(200)));
  }

  @Test
  void widensAnIntegerColumn() throws SQLException {
    apply(schema(orders(ID, STATE, column("3", "total", ColumnType.INT))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("state") + ", " + q("total")
        + ") VALUES (1, 'new', 7)");

    apply(schema(orders(ID, STATE, column("3", "total", ColumnType.BIGINT))));
    execute("UPDATE " + q("orders") + " SET " + q("total") + " = " + q("total") + " + ?", 5_000_000_000L);

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders"))).containsExactly(List.of("5000000007"));
  }

  @Test
  void makesAColumnRequiredAndFillsItsNulls() throws SQLException {
    apply(schema(orders(ID, STATE, nullable("3", "note", ColumnType.TEXT))));
    insertOrder();

    apply(schema(orders(ID, STATE, column("3", "note", ColumnType.TEXT))));

    assertThat(columns("orders")).containsEntry("note", false);
    assertThat(query("SELECT " + q("note") + " FROM " + q("orders"))).containsExactly(List.of(""));
  }

  @Test
  void makesAColumnOptional() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, new Column("2", "state", ColumnType.TEXT, 20, true))));
    execute("UPDATE " + q("orders") + " SET " + q("state") + " = NULL");

    assertThat(columns("orders")).containsEntry("state", true);
  }

  @Test
  void changesAnIndexedColumn() throws SQLException {
    apply(schema(table("shop.Order", "orders", List.of(ID, STATE), List.of("id"), List.of(Index.of("orders",
        List.of("state"), false)), List.of())));
    insertOrder();

    apply(schema(table("shop.Order", "orders", List.of(ID, text("2", "state", 200)), List.of("id"),
        List.of(Index.of("orders", List.of("state"), false)), List.of())));

    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_state", false));
    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }

  @Test
  void dropsAColumn() throws SQLException {
    apply(schema(orders(ID, STATE, nullable("3", "note", ColumnType.TEXT))));
    insertOrder();

    apply(schema(orders(ID, STATE)));

    assertThat(columns("orders")).containsOnlyKeys("id", "state");
    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }

  @Test
  void keepsTheDigitsOfADecimal() throws SQLException {
    apply(schema(orders(ID, column("2", "price", ColumnType.DECIMAL))));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("price") + ") VALUES (1, '1234567890.12')");

    assertThat(query("SELECT " + q("price") + " FROM " + q("orders"))).containsExactly(List.of("1234567890.12"));
  }

  @Test
  void storesDatesAndTimes() throws SQLException {
    apply(schema(orders(ID, column("2", "day", ColumnType.DATE), column("3", "hour", ColumnType.TIME))));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("day") + ", " + q("hour")
        + ") VALUES (1, '2026-10-14', '07:30:15')");

    assertThat(query("SELECT " + q("day") + ", " + q("hour") + " FROM " + q("orders")).getFirst())
        .satisfiesExactly(day -> assertThat(day).startsWith("2026-10-14"), hour -> assertThat(hour)
            .startsWith("07:30:15"));
  }

  @Test
  void createsAColumnWithACollation() throws SQLException {
    Column code = text("2", "code", 20).withCollations(Map.of(dialect.name(), testDatabase().binaryCollation()));
    apply(schema(table("shop.Order", "orders", List.of(ID, code), List.of("id"), List.of(Index.of("orders",
        List.of("code"), true)), List.of())));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("code") + ") VALUES (1, 'a')");
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("code") + ") VALUES (2, 'A')");

    assertThat(count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("code") + " = 'a'")).isEqualTo(1);
  }

  @Test
  void changesTheCollationOfAColumn() throws SQLException {
    Column code = text("2", "code", 20);
    apply(schema(table("shop.Order", "orders", List.of(ID, code), List.of("id"), List.of(Index.of("orders",
        List.of("code"), false)), List.of())));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("code") + ") VALUES (1, 'a')");

    apply(schema(table("shop.Order", "orders", List.of(ID, code.withCollations(Map.of(dialect.name(),
        testDatabase().binaryCollation()))), List.of("id"), List.of(Index.of("orders", List.of("code"), false)),
        List.of())));

    assertThat(count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("code") + " = 'A'")).isZero();
    assertThat(indexes("orders")).containsOnlyKeys("ix_orders_code");
  }

  @Test
  void addsAnIndexedColumnWithADefault() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(table("shop.Order", "orders", List.of(ID, STATE, column("3", "total", ColumnType.INT)
        .withDefault(Default.value("5"))), List.of("id"), List.of(Index.of("orders", List.of("total"), false)),
        List.of())));
    execute("UPDATE " + q("orders") + " SET " + q("total") + " = " + q("total") + " + 1");

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders"))).containsExactly(List.of("6"));
    assertThat(indexes("orders")).containsOnlyKeys("ix_orders_total");
  }
}
