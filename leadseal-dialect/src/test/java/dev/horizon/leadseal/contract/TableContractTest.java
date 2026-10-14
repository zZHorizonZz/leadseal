package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class TableContractTest extends ContractTest {

  private static final Table CUSTOMERS = table("shop.Customer", "customers", List.of(column("1", "id",
      BIGINT), text("2", "email", 100)), List.of("id"), List.of(
          IndexAttribute.of("customers", List.of("email"),
              true)),
      List.of());
  private static final Table ORDERS = table("shop.Order", "orders", List.of(column("1", "id", BIGINT),
      column("2", "customer_id", BIGINT), text("3", "state", 20)), List.of("id"),
      List.of(IndexAttribute.of("orders", List.of("state"), false)),
      List.of(ForeignKeyAttribute.of("orders", List.of("customer_id"),
          TableName.of("customers"), List.of("id"), ForeignKeyAttribute.Action.CASCADE)));

  @Test
  void createsTablesWithKeysAndIndexes() throws SQLException {
    apply(schema(ORDERS, CUSTOMERS));

    assertThat(columns("customers")).containsExactly(Map.entry("id", false), Map.entry("email", false));
    assertThat(columns("orders")).containsExactly(Map.entry("id", false), Map.entry("customer_id", false),
        Map.entry("state", false));
    assertThat(primaryKey("orders")).containsExactly("id");
    assertThat(indexes("customers")).containsExactly(Map.entry("ux_customers_email", true));
    assertThat(indexes("orders")).containsExactly(Map.entry("ix_orders_state", false));
    assertThat(foreignKeys("orders")).containsExactly("customer_id -> customers.id");
  }

  @Test
  void enforcesTheKeys() throws SQLException {
    apply(schema(ORDERS, CUSTOMERS));
    execute("INSERT INTO " + q("customers") + " (" + q("id") + ", " + q("email") + ") VALUES (1, 'a@example.com')");
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("customer_id") + ", " + q("state")
        + ") VALUES (1, 1, 'new')");

    assertThatThrownBy(() -> execute("INSERT INTO " + q("customers") + " (" + q("id") + ", " + q("email")
        + ") VALUES (2, 'a@example.com')")).isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("customer_id") + ", "
        + q("state") + ") VALUES (2, 9, 'new')")).isInstanceOf(SQLException.class);

    execute("DELETE FROM " + q("customers"));

    assertThat(query("SELECT " + q("id") + " FROM " + q("orders"))).isEmpty();
  }

  @Test
  void renamesATableAndKeepsItsRows() throws SQLException {
    apply(schema(CUSTOMERS));
    execute("INSERT INTO " + q("customers") + " (" + q("id") + ", " + q("email") + ") VALUES (1, 'a@example.com')");

    apply(schema(table("shop.Customer", "clients", CUSTOMERS.columns(), List.of("id"), List.of(IndexAttribute.of("clients",
        List.of("email"), true)), List.of())));

    assertThat(tableExists("customers")).isFalse();
    assertThat(query("SELECT " + q("email") + " FROM " + q("clients"))).containsExactly(List.of("a@example.com"));
    assertThat(primaryKey("clients")).containsExactly("id");
    assertThat(indexes("clients")).containsExactly(Map.entry("ux_clients_email", true));
  }

  @Test
  void dropsTablesThatReferenceEachOther() throws SQLException {
    apply(schema(ORDERS, CUSTOMERS));

    apply(Schema.empty());

    assertThat(tableExists("orders")).isFalse();
    assertThat(tableExists("customers")).isFalse();
  }

  @Test
  void runsEachMigrationOnce() {
    generate(schema(CUSTOMERS));

    assertThat(migrate()).isEqualTo(1);

    generate(schema(CUSTOMERS, ORDERS));

    assertThat(migrate()).isEqualTo(1);
    assertThat(migrate()).isZero();
  }
}
