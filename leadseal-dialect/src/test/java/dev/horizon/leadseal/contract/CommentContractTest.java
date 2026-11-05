package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Comment;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class CommentContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column STATE = text("2", "state", 20);

  private static Table orders(String comment, String stateComment) {
    Column state = stateComment == null ? STATE : STATE.with(new Comment(stateComment));
    Table orders = table("shop.Order", "orders", List.of(ID, state), "id");
    return comment == null ? orders : orders.with(new Comment(comment));
  }

  private String tableComment() throws SQLException {
    return testDatabase().tableComment(connection(), "orders");
  }

  private String stateComment() throws SQLException {
    return testDatabase().columnComment(connection(), "orders", "state");
  }

  @Test
  @DisplayName("Creates a table with comments")
  void createWithComments() throws SQLException {
    apply(schema(orders("The orders of a shop.", "Where the order is: new or sent.")));

    assertThat(tableComment()).isEqualTo("The orders of a shop.");
    assertThat(stateComment()).isEqualTo("Where the order is: new or sent.");
  }

  @Test
  @DisplayName("Changes the comments")
  void changeComments() throws SQLException {
    apply(schema(orders("Orders.", "State.")));

    apply(schema(orders("The orders of a shop. It's a table.", "The state.")));

    assertThat(tableComment()).isEqualTo("The orders of a shop. It's a table.");
    assertThat(stateComment()).isEqualTo("The state.");
  }

  @Test
  @DisplayName("Adds and removes comments")
  void addAndRemove() throws SQLException {
    apply(schema(orders(null, "State.")));

    apply(schema(orders("Orders.", null)));

    assertThat(tableComment()).isEqualTo("Orders.");
    assertThat(stateComment()).isNull();
  }

  @Test
  @DisplayName("Adds a column with a comment")
  void addCommentedColumn() throws SQLException {
    apply(schema(table("shop.Order", "orders", List.of(ID), "id")));

    apply(schema(orders(null, "State.")));

    assertThat(stateComment()).isEqualTo("State.");
  }

  @Test
  @DisplayName("Renames a commented column and adds a column with its name")
  void renameCommentedColumn() throws SQLException {
    apply(schema(orders(null, "State.")));

    apply(schema(table("shop.Order", "orders", List.of(ID, text("2", "status", 20).with(new Comment("State.")),
        nullable("3", "state", TEXT).with(new Comment("The new state."))), "id")));

    assertThat(testDatabase().columnComment(connection(), "orders", "status")).isEqualTo("State.");
    assertThat(stateComment()).isEqualTo("The new state.");
  }
}
