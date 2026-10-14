package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class SchemaContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column CUSTOMER_ID = column("2", "customer_id", BIGINT);
  private static final Column NAME = text("2", "name", 20);

  private static Table customers(String name) {
    return table("shop.Customer", name, List.of(ID, NAME), List.of("id"),
        List.of(IndexAttribute.of(name, List.of("name"), false)), List.of());
  }

  private static Table orders(TableName customers) {
    return table("shop.Order", "orders", List.of(ID, CUSTOMER_ID), List.of("id"), List.of(),
        List.of(ForeignKeyAttribute.of("orders", List.of("customer_id"), customers, List.of("id"),
            ForeignKeyAttribute.Action.CASCADE)));
  }

  private static Schema in(String namespace, Table... tables) {
    return Schema.of(new Namespace(namespace, List.of(tables)));
  }

  @BeforeEach
  void requireSchemas() {
    assumeTrue(testDatabase().supportsSchemas());
  }

  @Test
  void createsTablesInSchemas() throws SQLException {
    String sales = uniqueSchema();
    String crm = uniqueSchema();

    apply(Schema.of(new Namespace(crm, List.of(customers("customers"))),
        new Namespace(sales, List.of(orders(new TableName(crm, "customers"))))));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");
    execute("INSERT INTO " + q(sales, "orders") + " (" + q("id") + ", " + q("customer_id") + ") VALUES (1, 1)");

    assertThatThrownBy(() -> execute("INSERT INTO " + q(sales, "orders") + " (" + q("id") + ", "
        + q("customer_id") + ") VALUES (2, 9)")).isInstanceOf(SQLException.class);
    execute("DELETE FROM " + q(crm, "customers"));
    assertThat(count("SELECT COUNT(*) FROM " + q(sales, "orders"))).isZero();
  }

  @Test
  void allowsTheSameTableNameInTwoSchemas() throws SQLException {
    String sales = uniqueSchema();
    String archive = uniqueSchema();

    apply(Schema.of(new Namespace(sales, List.of(customers("customers"))),
        new Namespace(archive, List.of(table("shop.ArchivedCustomer", "customers", List.of(ID, NAME), "id")))));
    execute("INSERT INTO " + q(archive, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    assertThat(count("SELECT COUNT(*) FROM " + q(sales, "customers"))).isZero();
    assertThat(count("SELECT COUNT(*) FROM " + q(archive, "customers"))).isEqualTo(1);
  }

  @Test
  void movesATableToAnotherSchemaAndKeepsItsRows() throws SQLException {
    String crm = uniqueSchema();
    String archive = uniqueSchema();
    apply(in(crm, customers("customers")));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    apply(in(archive, customers("customers")));

    assertThat(query("SELECT " + q("name") + " FROM " + q(archive, "customers"))).containsExactly(List.of("Ada"));
    assertThatThrownBy(() -> query("SELECT " + q("name") + " FROM " + q(crm, "customers")))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void movesATableToTheDefaultSchema() throws SQLException {
    String crm = uniqueSchema();
    apply(in(crm, customers("customers")));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    apply(schema(customers("customers")));

    assertThat(query("SELECT " + q("name") + " FROM " + q("customers"))).containsExactly(List.of("Ada"));
  }

  @Test
  void renamesATableInASchema() throws SQLException {
    String crm = uniqueSchema();
    apply(in(crm, customers("customers"), orders(new TableName(crm, "customers"))));
    execute("INSERT INTO " + q(crm, "customers") + " (" + q("id") + ", " + q("name") + ") VALUES (1, 'Ada')");

    apply(in(crm, customers("clients"), orders(new TableName(crm, "clients"))));

    assertThat(query("SELECT " + q("name") + " FROM " + q(crm, "clients"))).containsExactly(List.of("Ada"));
    execute("INSERT INTO " + q(crm, "orders") + " (" + q("id") + ", " + q("customer_id") + ") VALUES (1, 1)");
  }
}
