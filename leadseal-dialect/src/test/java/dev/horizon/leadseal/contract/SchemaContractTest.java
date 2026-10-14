package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class SchemaContractTest extends ContractTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Column CUSTOMER_ID = column("2", "customer_id", ColumnType.BIGINT);
  private static final Column NAME = text("2", "name", 20);

  private static Table customers(String schema, String name) {
    return table("shop.Customer", name, List.of(ID, NAME), List.of("id"), List.of(Index.of(name, List.of("name"),
        false)), List.of()).withSchema(schema);
  }

  private static Table orders(String schema, String customers) {
    return table("shop.Order", "orders", List.of(ID, CUSTOMER_ID), List.of("id"), List.of(),
        List.of(ForeignKey.of("orders", List.of("customer_id"), customers, List.of("id"),
            ForeignKey.Action.CASCADE)))
        .withSchema(schema);
  }

  @BeforeEach
  void requireSchemas() {
    assumeTrue(testDatabase().supportsSchemas());
  }

  @Test
  void createsTablesInSchemas() throws SQLException {
    String sales = uniqueSchema();
    String crm = uniqueSchema();

    apply(schema(customers(crm, "customers"), orders(sales, "customers")));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");
    execute("INSERT INTO " + q(sales, "orders") + " (" + q("id") + ", " + q("customer_id") + ") VALUES (1, 1)");

    assertThatThrownBy(() -> execute("INSERT INTO " + q(sales, "orders") + " (" + q("id") + ", "
        + q("customer_id") + ") VALUES (2, 9)")).isInstanceOf(SQLException.class);
    execute("DELETE FROM " + q(crm, "customers"));
    assertThat(count("SELECT COUNT(*) FROM " + q(sales, "orders"))).isZero();
  }

  @Test
  void movesATableToAnotherSchemaAndKeepsItsRows() throws SQLException {
    String crm = uniqueSchema();
    String archive = uniqueSchema();
    apply(schema(customers(crm, "customers")));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    apply(schema(customers(archive, "customers")));

    assertThat(query("SELECT " + q("name") + " FROM " + q(archive, "customers"))).containsExactly(List.of("Ada"));
    assertThatThrownBy(() -> query("SELECT " + q("name") + " FROM " + q(crm, "customers")))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void movesATableToTheDefaultSchema() throws SQLException {
    String crm = uniqueSchema();
    apply(schema(customers(crm, "customers")));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    apply(schema(customers(null, "customers")));

    assertThat(query("SELECT " + q("name") + " FROM " + q("customers"))).containsExactly(List.of("Ada"));
  }

  @Test
  void renamesATableInASchema() throws SQLException {
    String crm = uniqueSchema();
    apply(schema(customers(crm, "customers"), orders(crm, "customers")));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    apply(schema(customers(crm, "clients"), orders(crm, "clients")));

    assertThat(query("SELECT " + q("name") + " FROM " + q(crm, "clients"))).containsExactly(List.of("Ada"));
    execute("INSERT INTO " + q(crm, "orders") + " (" + q("id") + ", " + q("customer_id") + ") VALUES (1, 1)");
  }
}
