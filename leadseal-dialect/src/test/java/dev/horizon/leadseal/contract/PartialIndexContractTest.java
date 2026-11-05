package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Index;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class PartialIndexContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column CUSTOMER_ID = column("2", "customer_id", BIGINT);
  private static final Column LINE = column("3", "line", INT);

  private static Table orders(Index index) {
    return table("shop.Order", "orders", List.of(ID, CUSTOMER_ID, LINE), List.of("id"), List.of(index), List.of());
  }

  private void insert(String table, long... values) throws SQLException {
    StringBuilder sql = new StringBuilder("INSERT INTO " + q(table) + " VALUES (");
    for (int i = 0; i < values.length; i++) {
      sql.append(i == 0 ? "" : ", ").append(values[i]);
    }
    execute(sql.append(")").toString());
  }

  @Test
  @DisplayName("Creates a partial unique index")
  void createPartialUnique() throws SQLException {
    Index index = Index.of("orders", List.of("customer_id"), true).withWhere("line = 1");
    apply(schema(orders(index)));
    insert("orders", 1, 1, 1);
    insert("orders", 2, 1, 2);
    insert("orders", 3, 1, 3);

    assertThatThrownBy(() -> insert("orders", 4, 1, 1)).isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("Changes the condition of a partial index")
  void changeCondition() throws SQLException {
    Index index = Index.of("orders", List.of("customer_id"), true);
    apply(schema(orders(index.withWhere("line = 1"))));
    insert("orders", 1, 1, 1);
    insert("orders", 2, 1, 2);

    apply(schema(orders(index.withWhere("line = 2"))));

    insert("orders", 3, 1, 3);
    assertThatThrownBy(() -> insert("orders", 4, 1, 2)).isInstanceOf(SQLException.class);
  }
}
