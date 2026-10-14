package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class KeyContractTest extends ContractTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Column CUSTOMER_ID = column("2", "customer_id", ColumnType.BIGINT);
  private static final Column LINE = column("3", "line", ColumnType.INT);
  private static final Table CUSTOMERS = table("shop.Customer", "customers", List.of(ID), "id");

  private static Table orders(List<Index> indexes, List<ForeignKey> keys) {
    return table("shop.Order", "orders", List.of(ID, CUSTOMER_ID, LINE), List.of("id"), indexes, keys);
  }

  private static ForeignKey toCustomers(ForeignKey.Action action) {
    return ForeignKey.of("orders", List.of("customer_id"), "customers", List.of("id"), action);
  }

  private void insert(String table, long... values) throws SQLException {
    StringBuilder sql = new StringBuilder("INSERT INTO " + q(table) + " VALUES (");
    for (int i = 0; i < values.length; i++) {
      sql.append(i == 0 ? "" : ", ").append(values[i]);
    }
    execute(sql.append(")").toString());
  }

  @Test
  void changesThePrimaryKey() throws SQLException {
    apply(schema(orders(List.of(), List.of())));
    insert("orders", 1, 1, 1);

    apply(schema(table("shop.Order", "orders", List.of(ID, CUSTOMER_ID, LINE), List.of("id", "line"), List.of(),
        List.of())));
    insert("orders", 1, 1, 2);

    assertThat(primaryKey("orders")).containsExactly("id", "line");
    assertThatThrownBy(() -> insert("orders", 1, 1, 2)).isInstanceOf(SQLException.class);
  }

  @Test
  void addsAForeignKeyToAnExistingTable() throws SQLException {
    apply(schema(CUSTOMERS, orders(List.of(), List.of())));
    insert("customers", 1);
    insert("orders", 1, 1, 1);

    apply(schema(CUSTOMERS, orders(List.of(), List.of(toCustomers(ForeignKey.Action.RESTRICT)))));

    assertThat(foreignKeys("orders")).containsExactly("customer_id -> customers.id");
    assertThat(query("SELECT " + q("id") + " FROM " + q("orders"))).containsExactly(List.of("1"));
    assertThatThrownBy(() -> insert("orders", 2, 9, 1)).isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> execute("DELETE FROM " + q("customers"))).isInstanceOf(SQLException.class);
  }

  @ParameterizedTest
  @EnumSource(ForeignKey.Action.class)
  void appliesTheDeleteAction(ForeignKey.Action action) throws SQLException {
    Column customer = action == ForeignKey.Action.SET_NULL ? nullable("2", "customer_id", ColumnType.BIGINT)
        : CUSTOMER_ID;
    apply(schema(CUSTOMERS, table("shop.Order", "orders", List.of(ID, customer, LINE), List.of("id"), List.of(),
        List.of(toCustomers(action)))));
    insert("customers", 1);
    insert("orders", 1, 1, 1);

    if (action == ForeignKey.Action.CASCADE || action == ForeignKey.Action.SET_NULL) {
      execute("DELETE FROM " + q("customers"));
    } else {
      assertThatThrownBy(() -> execute("DELETE FROM " + q("customers"))).isInstanceOf(SQLException.class);
    }

    List<List<String>> rows = query("SELECT " + q("customer_id") + " FROM " + q("orders"));
    switch (action) {
      case CASCADE -> assertThat(rows).isEmpty();
      case SET_NULL -> assertThat(rows).containsExactly(Collections.singletonList(null));
      default -> assertThat(rows).containsExactly(List.of("1"));
    }
  }

  @Test
  void dropsAForeignKey() throws SQLException {
    apply(schema(CUSTOMERS, orders(List.of(), List.of(toCustomers(ForeignKey.Action.CASCADE)))));
    insert("customers", 1);
    insert("orders", 1, 1, 1);

    apply(schema(CUSTOMERS, orders(List.of(), List.of())));
    insert("orders", 2, 9, 1);

    assertThat(foreignKeys("orders")).isEmpty();
    assertThat(query("SELECT " + q("id") + " FROM " + q("orders") + " ORDER BY " + q("id")))
        .containsExactly(List.of("1"), List.of("2"));
  }

  @Test
  void addsAndDropsIndexes() throws SQLException {
    apply(schema(orders(List.of(), List.of())));
    insert("orders", 1, 1, 1);

    apply(schema(orders(List.of(Index.of("orders", List.of("customer_id", "line"), true), Index.of("orders",
        List.of("line"), false)), List.of())));

    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_line", false),
        Map.entry("ux_orders_customer_id_line", true));
    assertThatThrownBy(() -> insert("orders", 2, 1, 1)).isInstanceOf(SQLException.class);

    apply(schema(orders(List.of(), List.of())));
    insert("orders", 2, 1, 1);

    assertThat(indexes("orders")).isEmpty();
  }

  @Test
  void keepsTheRowsThatReferenceAChangedTable() throws SQLException {
    Table customers = table("shop.Customer", "customers", List.of(ID, text("2", "name", 20)), "id");
    apply(schema(customers, orders(List.of(), List.of(toCustomers(ForeignKey.Action.CASCADE)))));
    execute("INSERT INTO " + q("customers") + " VALUES (1, 'Ada')");
    insert("orders", 1, 1, 1);

    apply(schema(table("shop.Customer", "customers", List.of(ID, text("2", "name", 40)), "id"),
        orders(List.of(), List.of(toCustomers(ForeignKey.Action.CASCADE)))));

    assertThat(query("SELECT " + q("id") + " FROM " + q("orders"))).containsExactly(List.of("1"));
    assertThat(foreignKeys("orders")).containsExactly("customer_id -> customers.id");
  }
}
