package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class ColumnContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column STATE = text("2", "state", 20);

  private static Table orders(Column... columns) {
    return table("shop.Order", "orders", List.of(columns), "id");
  }

  private void insertOrder() throws SQLException {
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("state") + ") VALUES (1, 'new')");
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("types")
  @DisplayName("Creates a column of each type")
  void createEachType(Type type) throws SQLException {
    apply(schema(orders(ID, column("2", "value", type), nullable("3", "optional", type))));

    assertThat(columns("orders")).containsExactly(Map.entry("id", false), Map.entry("value", false),
        Map.entry("optional", true));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("types")
  @DisplayName("Adds a required column to a table with rows")
  void addRequired(Type type) throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, STATE, column("3", "value", type))));

    assertThat(columns("orders")).containsEntry("value", false);
    assertThat(query("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("value") + " IS NOT NULL"))
        .containsExactly(List.of("1"));
  }

  @Test
  @DisplayName("Adds an optional column")
  void addOptional() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, STATE, nullable("3", "note", TEXT))));

    assertThat(query("SELECT " + q("state") + ", " + q("note") + " FROM " + q("orders")))
        .containsExactly(Arrays.asList("new", null));
  }

  @Test
  @DisplayName("Renames a column and keeps its values")
  void renameColumn() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, text("2", "status", 20))));

    assertThat(columns("orders")).containsOnlyKeys("id", "status");
    assertThat(query("SELECT " + q("status") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }

  @Test
  @DisplayName("Widens a text column")
  void widenText() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, text("2", "state", 200))));
    execute("UPDATE " + q("orders") + " SET " + q("state") + " = ?", "x".repeat(200));

    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("x".repeat(200)));
  }

  @Test
  @DisplayName("Widens an integer column")
  void widenInteger() throws SQLException {
    apply(schema(orders(ID, STATE, column("3", "total", INT))));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("state") + ", " + q("total")
        + ") VALUES (1, 'new', 7)");

    apply(schema(orders(ID, STATE, column("3", "total", BIGINT))));
    execute("UPDATE " + q("orders") + " SET " + q("total") + " = " + q("total") + " + ?", 5_000_000_000L);

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders"))).containsExactly(List.of("5000000007"));
  }

  @Test
  @DisplayName("Makes a column required and fills its nulls")
  void makeRequired() throws SQLException {
    apply(schema(orders(ID, STATE, nullable("3", "note", TEXT))));
    insertOrder();

    apply(schema(orders(ID, STATE, column("3", "note", TEXT))));

    assertThat(columns("orders")).containsEntry("note", false);
    assertThat(query("SELECT " + q("note") + " FROM " + q("orders"))).containsExactly(List.of(""));
  }

  @Test
  @DisplayName("Makes a column optional")
  void makeOptional() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(orders(ID, Column.of("2", "state", TextType.of(20)))));
    execute("UPDATE " + q("orders") + " SET " + q("state") + " = NULL");

    assertThat(columns("orders")).containsEntry("state", true);
  }

  @Test
  @DisplayName("Changes an indexed column")
  void changeIndexed() throws SQLException {
    apply(schema(table("shop.Order", "orders", List.of(ID, STATE), List.of("id"), List.of(Index.of("orders",
        List.of("state"), false)), List.of())));
    insertOrder();

    apply(schema(table("shop.Order", "orders", List.of(ID, text("2", "state", 200)), List.of("id"),
        List.of(Index.of("orders", List.of("state"), false)), List.of())));

    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_state", false));
    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }

  @Test
  @DisplayName("Drops a column")
  void dropColumn() throws SQLException {
    apply(schema(orders(ID, STATE, nullable("3", "note", TEXT))));
    insertOrder();

    apply(schema(orders(ID, STATE)));

    assertThat(columns("orders")).containsOnlyKeys("id", "state");
    assertThat(query("SELECT " + q("state") + " FROM " + q("orders"))).containsExactly(List.of("new"));
  }

  @Test
  @DisplayName("Keeps the digits of a decimal")
  void keepDecimalDigits() throws SQLException {
    apply(schema(orders(ID, column("2", "price", DECIMAL))));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("price") + ") VALUES (1, '1234567890.12')");

    assertThat(query("SELECT " + q("price") + " FROM " + q("orders"))).containsExactly(List.of("1234567890.12"));
  }

  @Test
  @DisplayName("Stores dates and times")
  void storeDatesAndTimes() throws SQLException {
    apply(schema(orders(ID, column("2", "day", DATE), column("3", "hour", TIME))));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("day") + ", " + q("hour")
        + ") VALUES (1, '2026-10-14', '07:30:15')");

    assertThat(query("SELECT " + q("day") + ", " + q("hour") + " FROM " + q("orders")).getFirst())
        .satisfiesExactly(day -> assertThat(day).startsWith("2026-10-14"), hour -> assertThat(hour)
            .startsWith("07:30:15"));
  }

  @Test
  @DisplayName("Creates a column with a collation")
  void createWithCollation() throws SQLException {
    Column code = text("2", "code", 20).with(new Collation(dialect.name(), testDatabase().binaryCollation()));
    apply(schema(table("shop.Order", "orders", List.of(ID, code), List.of("id"), List.of(Index.of("orders",
        List.of("code"), true)), List.of())));

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("code") + ") VALUES (1, 'a')");
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("code") + ") VALUES (2, 'A')");

    assertThat(count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("code") + " = 'a'")).isEqualTo(1);
  }

  @Test
  @DisplayName("Changes the collation of a column")
  void changeCollation() throws SQLException {
    Column code = text("2", "code", 20);
    apply(schema(table("shop.Order", "orders", List.of(ID, code), List.of("id"), List.of(Index.of("orders",
        List.of("code"), false)), List.of())));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("code") + ") VALUES (1, 'a')");

    apply(schema(table("shop.Order", "orders",
        List.of(ID, code.with(new Collation(dialect.name(), testDatabase().binaryCollation()))), List.of("id"),
        List.of(Index.of("orders", List.of("code"), false)),
        List.of())));

    assertThat(count("SELECT COUNT(*) FROM " + q("orders") + " WHERE " + q("code") + " = 'A'")).isZero();
    assertThat(indexes("orders")).containsOnlyKeys("ix_orders_code");
  }

  @Test
  @DisplayName("Adds an indexed column with a default")
  void addIndexedWithDefault() throws SQLException {
    apply(schema(orders(ID, STATE)));
    insertOrder();

    apply(schema(table("shop.Order", "orders", List.of(ID, STATE, column("3", "total", INT)
        .with(new LiteralDefault("5"))), List.of("id"),
        List.of(Index.of("orders", List.of("total"), false)),
        List.of())));
    execute("UPDATE " + q("orders") + " SET " + q("total") + " = " + q("total") + " + 1");

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders"))).containsExactly(List.of("6"));
    assertThat(indexes("orders")).containsOnlyKeys("ix_orders_total");
  }

  @Test
  @DisplayName("Retypes two columns of one index")
  void retypeIndexColumns() throws SQLException {
    Index index = new Index("ix_orders_count_line", List.of("count", "line"), false, List.of(), Index.Rows.ALL);
    apply(schema(table("shop.Order", "orders", List.of(ID, column("3", "count", INT), column("4", "line", INT)),
        List.of("id"), List.of(index), List.of())));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("count") + ", " + q("line") + ") VALUES (1, 7,"
        + " 2)");

    apply(schema(table("shop.Order", "orders", List.of(ID, column("3", "count", BIGINT), column("4", "line", BIGINT)),
        List.of("id"), List.of(index), List.of())));

    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_count_line", false));
    assertThat(query("SELECT " + q("count") + ", " + q("line") + " FROM " + q("orders")))
        .containsExactly(List.of("7", "2"));
  }

  @Test
  @DisplayName("Renames and retypes an indexed column")
  void renameAndRetypeIndexed() throws SQLException {
    apply(schema(table("shop.Order", "orders", List.of(ID, column("3", "count", INT)), List.of("id"),
        List.of(new Index("ix_orders_count", List.of("count"), false, List.of(), Index.Rows.ALL)), List.of())));
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("count") + ") VALUES (1, 7)");

    apply(schema(table("shop.Order", "orders", List.of(ID, column("3", "total", BIGINT)), List.of("id"),
        List.of(new Index("ix_orders_count", List.of("total"), false, List.of(), Index.Rows.ALL)), List.of())));

    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_count", false));
    assertThat(query("SELECT " + q("total") + " FROM " + q("orders"))).containsExactly(List.of("7"));
  }
}
