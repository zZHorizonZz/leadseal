package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Check;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class CheckContractTest extends ContractTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Column QUANTITY = column("2", "quantity", ColumnType.INT);

  private static Table orders(String... checks) {
    return table("shop.Order", "orders", List.of(ID, QUANTITY), "id")
        .withChecks(List.of(checks).stream().map(check -> Check.of("orders", check)).toList());
  }

  private void insert(long id, int quantity) throws SQLException {
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("quantity") + ") VALUES (?, ?)", id, quantity);
  }

  @Test
  void createsATableWithChecks() throws SQLException {
    apply(schema(orders("quantity >= 0")));

    insert(1, 0);

    assertThatThrownBy(() -> insert(2, -1)).isInstanceOf(SQLException.class);
  }

  @Test
  void addsACheckToAnExistingTable() throws SQLException {
    apply(schema(orders()));
    insert(1, 5);

    apply(schema(orders("quantity >= 0")));

    assertThatThrownBy(() -> insert(2, -1)).isInstanceOf(SQLException.class);
    assertThat(query("SELECT " + q("quantity") + " FROM " + q("orders"))).containsExactly(List.of("5"));
  }

  @Test
  void changesACheck() throws SQLException {
    apply(schema(orders("quantity >= 0")));
    insert(1, 5);

    apply(schema(orders("quantity >= 0 AND quantity <= 10")));

    insert(2, 10);
    assertThatThrownBy(() -> insert(3, 11)).isInstanceOf(SQLException.class);
  }

  @Test
  void dropsACheck() throws SQLException {
    apply(schema(orders("quantity >= 0")));

    apply(schema(orders()));

    insert(1, -1);
    assertThat(count("SELECT COUNT(*) FROM " + q("orders"))).isEqualTo(1);
  }
}
