package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class KeyContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column CUSTOMER_ID = column("2", "customer_id", BIGINT);
  private static final Column LINE = column("3", "line", INT);
  private static final Table CUSTOMERS = table("shop.Customer", "customers", List.of(ID), "id");

  private static Table orders(List<Index> indexes, List<ForeignKey> keys) {
    return table("shop.Order", "orders", List.of(ID, CUSTOMER_ID, LINE), List.of("id"), indexes, keys);
  }

  private static ForeignKey toCustomers(ForeignKey.Action action) {
    return ForeignKey.of("orders", List.of("customer_id"), TableName.of("customers"), List.of("id"), action);
  }

  private void insert(String table, long... values) throws SQLException {
    StringBuilder sql = new StringBuilder("INSERT INTO " + q(table) + " VALUES (");
    for (int i = 0; i < values.length; i++) {
      sql.append(i == 0 ? "" : ", ").append(values[i]);
    }
    execute(sql.append(")").toString());
  }

  @Test
  @DisplayName("Changes the primary key")
  void changePrimaryKey() throws SQLException {
    apply(schema(orders(List.of(), List.of())));
    insert("orders", 1, 1, 1);

    apply(schema(table("shop.Order", "orders", List.of(ID, CUSTOMER_ID, LINE), List.of("id", "line"), List.of(),
        List.of())));
    insert("orders", 1, 1, 2);

    assertThat(primaryKey("orders")).containsExactly("id", "line");
    assertThatThrownBy(() -> insert("orders", 1, 1, 2)).isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("Adds a foreign key to an existing table")
  void addForeignKey() throws SQLException {
    apply(schema(CUSTOMERS, orders(List.of(), List.of())));
    insert("customers", 1);
    insert("orders", 1, 1, 1);

    apply(schema(CUSTOMERS, orders(List.of(), List.of(toCustomers(ForeignKey.Action.RESTRICT)))));

    assertThat(foreignKeys("orders")).containsExactly("customer_id -> customers.id");
    assertThat(query("SELECT " + q("id") + " FROM " + q("orders"))).containsExactly(List.of("1"));
    assertThatThrownBy(() -> insert("orders", 2, 9, 1)).isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> execute("DELETE FROM " + q("customers"))).isInstanceOf(SQLException.class);
  }

  @ParameterizedTest(name = "({0})")
  @EnumSource(ForeignKey.Action.class)
  @DisplayName("Applies the delete action")
  void applyDeleteAction(ForeignKey.Action action) throws SQLException {
    Column customer = action == ForeignKey.Action.SET_NULL ? nullable("2", "customer_id", BIGINT)
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
  @DisplayName("Drops a foreign key")
  void dropForeignKey() throws SQLException {
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
  @DisplayName("Adds and drops indexes")
  void addAndDropIndexes() throws SQLException {
    apply(schema(orders(List.of(), List.of())));
    insert("orders", 1, 1, 1);

    apply(schema(orders(
        List.of(Index.of("orders", List.of("customer_id", "line"), true), Index.of("orders",
            List.of("line"), false)),
        List.of())));

    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_line", false),
        Map.entry("ux_orders_customer_id_line", true));
    assertThatThrownBy(() -> insert("orders", 2, 1, 1)).isInstanceOf(SQLException.class);

    apply(schema(orders(List.of(), List.of())));
    insert("orders", 2, 1, 1);

    assertThat(indexes("orders")).isEmpty();
  }

  @Test
  @DisplayName("Keeps the rows that reference a changed table")
  void keepReferencingRows() throws SQLException {
    Table customers = table("shop.Customer", "customers", List.of(ID, text("2", "name", 20)), "id");
    apply(schema(customers, orders(List.of(), List.of(toCustomers(ForeignKey.Action.CASCADE)))));
    execute("INSERT INTO " + q("customers") + " VALUES (1, 'Ada')");
    insert("orders", 1, 1, 1);

    apply(schema(table("shop.Customer", "customers", List.of(ID, text("2", "name", 40)), "id"),
        orders(List.of(), List.of(toCustomers(ForeignKey.Action.CASCADE)))));

    assertThat(query("SELECT " + q("id") + " FROM " + q("orders"))).containsExactly(List.of("1"));
    assertThat(foreignKeys("orders")).containsExactly("customer_id -> customers.id");
  }

  @Test
  @DisplayName("Creates a descending index")
  void createDescendingIndex() throws SQLException {
    Index index = Index.of("orders", List.of("customer_id", "line"), false)
        .withDescending(List.of("line"));
    apply(schema(orders(List.of(index), List.of())));
    insert("orders", 1, 1, 1);

    assertThat(indexes("orders")).containsOnlyKeys("ix_orders_customer_id_line");
  }

  @Test
  @DisplayName("Updates referencing rows")
  void updateReferencingRows() throws SQLException {
    ForeignKey key = toCustomers(ForeignKey.Action.CASCADE)
        .withOnUpdate(ForeignKey.Action.CASCADE);
    apply(schema(CUSTOMERS, orders(List.of(), List.of(key))));
    insert("customers", 1);
    insert("orders", 1, 1, 1);

    execute("UPDATE " + q("customers") + " SET " + q("id") + " = 2");

    assertThat(query("SELECT " + q("customer_id") + " FROM " + q("orders"))).containsExactly(List.of("2"));
  }

  @Test
  @DisplayName("Creates a composite foreign key")
  void createCompositeForeignKey() throws SQLException {
    Table lines = table("shop.OrderLine", "order_line", List.of(ID, CUSTOMER_ID, LINE), List.of("customer_id",
        "line"), List.of(), List.of());
    ForeignKey key = ForeignKey.of("orders", List.of("customer_id", "line"),
        TableName.of("order_line"),
        List.of("customer_id",
            "line"),
        ForeignKey.Action.CASCADE);
    apply(schema(lines, orders(List.of(), List.of(key))));
    insert("order_line", 1, 1, 1);
    insert("orders", 1, 1, 1);

    assertThatThrownBy(() -> insert("orders", 2, 1, 2)).isInstanceOf(SQLException.class);
    execute("DELETE FROM " + q("order_line"));
    assertThat(count("SELECT COUNT(*) FROM " + q("orders"))).isZero();
  }

  @Test
  @DisplayName("Changes the primary key of a renamed table")
  void changeRenamedTablePrimaryKey() throws SQLException {
    apply(schema(orders(List.of(), List.of())));
    insert("orders", 1, 1, 1);

    apply(schema(table("shop.Order", "purchases", List.of(ID, CUSTOMER_ID, LINE), List.of("id", "line"), List.of(),
        List.of())));
    insert("purchases", 1, 1, 2);

    assertThat(primaryKey("purchases")).containsExactly("id", "line");
  }

  @Test
  @DisplayName("Changes a renamed column of a foreign key")
  void changeRenamedForeignKeyColumn() throws SQLException {
    apply(schema(CUSTOMERS, orders(List.of(), List.of(toCustomers(ForeignKey.Action.NO_ACTION)
        .withName("fk_orders_customer")))));
    insert("customers", 1);
    insert("orders", 1, 1, 1);

    apply(schema(CUSTOMERS, table("shop.Order", "orders", List.of(ID, nullable("2", "buyer_id", BIGINT), LINE),
        List.of("id"), List.of(), List.of(ForeignKey.of("orders", List.of("buyer_id"), TableName.of("customers"),
            List.of("id"), ForeignKey.Action.NO_ACTION).withName("fk_orders_customer")))));

    assertThat(foreignKeys("orders")).containsExactly("buyer_id -> customers.id");
    assertThat(columns("orders")).containsEntry("buyer_id", true);
  }
}
