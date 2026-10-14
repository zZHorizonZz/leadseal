package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class CommentContractTest extends ContractTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Column STATE = text("2", "state", 20);

  private static Table orders(String comment, String stateComment) {
    return table("shop.Order", "orders", List.of(ID, STATE.withComment(stateComment)), "id").withComment(comment);
  }

  private String tableComment() throws SQLException {
    return testDatabase().tableComment(connection(), "orders");
  }

  private String stateComment() throws SQLException {
    return testDatabase().columnComment(connection(), "orders", "state");
  }

  @BeforeEach
  void requireComments() {
    assumeTrue(testDatabase().supportsComments());
  }

  @Test
  void createsATableWithComments() throws SQLException {
    apply(schema(orders("The orders of a shop.", "Where the order is: new or sent.")));

    assertThat(tableComment()).isEqualTo("The orders of a shop.");
    assertThat(stateComment()).isEqualTo("Where the order is: new or sent.");
  }

  @Test
  void changesTheComments() throws SQLException {
    apply(schema(orders("Orders.", "State.")));

    apply(schema(orders("The orders of a shop. It's a table.", "The state.")));

    assertThat(tableComment()).isEqualTo("The orders of a shop. It's a table.");
    assertThat(stateComment()).isEqualTo("The state.");
  }

  @Test
  void addsAndRemovesComments() throws SQLException {
    apply(schema(orders(null, "State.")));

    apply(schema(orders("Orders.", null)));

    assertThat(tableComment()).isEqualTo("Orders.");
    assertThat(stateComment()).isNull();
  }

  @Test
  void addsAColumnWithAComment() throws SQLException {
    apply(schema(table("shop.Order", "orders", List.of(ID), "id")));

    apply(schema(orders(null, "State.")));

    assertThat(stateComment()).isEqualTo("State.");
  }
}
