package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.ForeignColumn;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.type.TextType;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class TriggerContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column TOTAL = column("3", "total", INT).with(new LiteralDefault("0"));
  private static final Column AUTHOR = nullable("4", "author_id", BIGINT);
  private static final Column REVISION = column("5", "revision", INT).with(new LiteralDefault("0"));

  private static Table authors() {
    return table("shop.Author", "authors", List.of(ID, column("2", "active", BOOLEAN), column("3", "orders", INT).with(
        new LiteralDefault("0"))), "id");
  }

  private static Table eventTable(String name) {
    return eventTable(name, 20);
  }

  private static Table eventTable(String name, int state) {
    return table("shop.OrderEvent", name, List.of(ID.with(Identity.INSTANCE), column("2", "order_id", BIGINT),
        nullable("3", "state", TextType.of(state)), nullable("4", "revision", INT)), "id");
  }

  private static Table orders(Column state, String events, boolean triggers) {
    Table table = table("shop.Order", "orders", List.of(ID, state, TOTAL, AUTHOR, REVISION), "id").with(ForeignKey.of(
        "orders", List.of("author_id"), TableName.of("authors"), List.of("id"), ForeignKey.Action.NO_ACTION));
    return triggers ? table.with(triggers(events)) : table;
  }

  private static Triggers triggers(String events) {
    TableName log = TableName.of(events);
    return new Triggers(List.of(new Rewrite("revision", new Arithmetic(Arithmetic.Operator.ADD, new ColumnReference(
        "revision", Row.OLD), new Literal(INT, "1")))), List.of(
            new Trigger("created", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Insert(log, List.of(
                new Trigger.Assignment("order_id", ColumnReference.of("id")), new Trigger.Assignment("state",
                    ColumnReference.of("state")))))),
            new Trigger("log", List.of(Mutation.UPDATE), new ColumnChange("state"), List.of(new Trigger.Insert(log, List.of(
                new Trigger.Assignment("order_id", ColumnReference.of("id")), new Trigger.Assignment("state",
                    ColumnReference.of("state")),
                new Trigger.Assignment("revision", ColumnReference.of("revision")))))),
            new Trigger("positive", List.of(Mutation.INSERT, Mutation.UPDATE), Trigger.ALWAYS, List.of(new Trigger.Assert(
                new Comparison(Comparison.Operator.GREATER_EQUALS, ColumnReference.of("total"), new Literal(INT, "0")),
                "The total is negative."))),
            new Trigger("active", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Assert(new ForeignColumn(
                ColumnReference.of("author_id"), TableName.of("authors"), "id", "active"), "The author is not active."))),
            new Trigger("paid", List.of(Mutation.DELETE), Trigger.ALWAYS, List.of(new Trigger.Assert(new Comparison(
                Comparison.Operator.NOT_EQUALS, new ColumnReference("state", Row.OLD), new Literal(TextType.of(20),
                    "PAID")),
                "Paid orders stay."))),
            new Trigger("count", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(count(Row.NEW,
                Arithmetic.Operator.ADD))),
            new Trigger("purge", List.of(Mutation.DELETE), Trigger.ALWAYS, List.of(new Trigger.Delete(log,
                new Comparison(Comparison.Operator.EQUALS, ColumnReference.of("order_id"), new ColumnReference("id",
                    Row.OLD))),
                count(Row.OLD, Arithmetic.Operator.SUBTRACT)))));
  }

  private static Trigger.Update count(Row row, Arithmetic.Operator operator) {
    return new Trigger.Update(TableName.of("authors"), new Comparison(Comparison.Operator.EQUALS, ColumnReference.of(
        "id"), new ColumnReference("author_id", row)), List.of(
            new Trigger.Assignment("orders", new Arithmetic(operator,
                ColumnReference.of("orders"), new Literal(INT, "1")))));
  }

  private static Schema shop(boolean triggers) {
    return schema(authors(), eventTable("order_events"), orders(text("2", "state", 20), "order_events", triggers));
  }

  private void insert(long id, String state, Integer total, Long author) throws SQLException {
    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("state") + ", " + q("total") + ", " + q(
        "author_id") + ") VALUES (?, ?, ?, ?)", id, state, total, author);
  }

  private void insert(long id, String state) throws SQLException {
    insert(id, state, 0, null);
  }

  private List<List<String>> events(String table) throws SQLException {
    return query("SELECT " + q("order_id") + ", " + q("state") + ", " + q("revision") + " FROM " + q(table)
        + " ORDER BY " + q("id"));
  }

  @Test
  @DisplayName("Writes a row into another table after an insert")
  void insertRow() throws SQLException {
    apply(shop(true));

    insert(1, "NEW");

    assertThat(events("order_events")).containsExactly(Arrays.asList("1", "NEW", null));
  }

  @Test
  @DisplayName("Writes a row after an update that meets the condition, with the rewritten values, once")
  void updateRow() throws SQLException {
    apply(shop(true));
    insert(1, "NEW");

    execute("UPDATE " + q("orders") + " SET " + q("total") + " = 5");
    execute("UPDATE " + q("orders") + " SET " + q("state") + " = ?", "PAID");

    assertThat(events("order_events")).containsExactly(Arrays.asList("1", "NEW", null), List.of("1", "PAID",
        "2"));
  }

  @Test
  @DisplayName("Writes a row for each row of an update")
  void updateRows() throws SQLException {
    apply(shop(true));
    insert(1, "NEW");
    insert(2, "NEW");

    execute("UPDATE " + q("orders") + " SET " + q("state") + " = ?", "SENT");

    assertThat(count("SELECT COUNT(*) FROM " + q("order_events") + " WHERE " + q("state") + " = ?", "SENT"))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("Refuses a delete that an assert refuses, with the message of the assert")
  void refuseDelete() throws SQLException {
    apply(shop(true));
    insert(1, "PAID");
    insert(2, "NEW");

    assertThatThrownBy(() -> execute("DELETE FROM " + q("orders") + " WHERE " + q("id") + " = 1"))
        .isInstanceOf(SQLException.class).hasMessageContaining("Paid orders stay.");
    execute("DELETE FROM " + q("orders") + " WHERE " + q("id") + " = 2");

    assertThat(query("SELECT " + q("id") + " FROM " + q("orders"))).containsExactly(List.of("1"));
  }

  @Test
  @DisplayName("Refuses an insert and an update that an assert refuses")
  void refuseWrite() throws SQLException {
    apply(shop(true));
    insert(1, "NEW");

    assertThatThrownBy(() -> insert(2, "NEW", -1, null)).isInstanceOf(SQLException.class).hasMessageContaining(
        "The total is negative.");
    assertThatThrownBy(() -> execute("UPDATE " + q("orders") + " SET " + q("total") + " = -1"))
        .isInstanceOf(SQLException.class).hasMessageContaining("The total is negative.");

    assertThat(query("SELECT " + q("total") + " FROM " + q("orders"))).containsExactly(List.of("0"));
  }

  @Test
  @DisplayName("Reads the row that a key references, and lets a missing row pass")
  void readForeignRow() throws SQLException {
    apply(shop(true));
    execute("INSERT INTO " + q("authors") + " (" + q("id") + ", " + q("active") + ") VALUES (?, ?), (?, ?)", 1L, true,
        2L, false);

    insert(1, "NEW", 0, 1L);
    insert(2, "NEW", 0, null);

    assertThatThrownBy(() -> insert(3, "NEW", 0, 2L)).isInstanceOf(SQLException.class).hasMessageContaining(
        "The author is not active.");
  }

  @Test
  @DisplayName("Changes a row of another table once for each row of an insert")
  void updateOtherTable() throws SQLException {
    apply(shop(true));
    execute("INSERT INTO " + q("authors") + " (" + q("id") + ", " + q("active") + ") VALUES (?, ?)", 1L, true);

    execute("INSERT INTO " + q("orders") + " (" + q("id") + ", " + q("state") + ", " + q("author_id") + ") VALUES (?, ?,"
        + " ?), (?, ?, ?)", 1L, "NEW", 1L, 2L, "NEW", 1L);

    assertThat(query("SELECT " + q("orders") + " FROM " + q("authors"))).containsExactly(List.of("2"));
  }

  @Test
  @DisplayName("Runs several actions in their order: deletes the rows of another table and changes others")
  void deleteOtherRows() throws SQLException {
    apply(shop(true));
    execute("INSERT INTO " + q("authors") + " (" + q("id") + ", " + q("active") + ") VALUES (?, ?)", 1L, true);
    insert(1, "NEW", 0, 1L);
    insert(2, "NEW", 0, 1L);

    execute("DELETE FROM " + q("orders") + " WHERE " + q("id") + " = 1");

    assertThat(query("SELECT " + q("order_id") + " FROM " + q("order_events"))).containsExactly(List.of("2"));
    assertThat(query("SELECT " + q("orders") + " FROM " + q("authors"))).containsExactly(List.of("1"));
  }

  @Test
  @DisplayName("Keeps writing rows after a migration renames the table that the triggers write into")
  void renameTarget() throws SQLException {
    apply(shop(true));

    apply(schema(authors(), eventTable("events"), orders(text("2", "state", 20), "events", true)));
    insert(1, "NEW");

    assertThat(events("events")).containsExactly(Arrays.asList("1", "NEW", null));
  }

  @Test
  @DisplayName("Keeps the triggers working after a migration changes a table that they write into")
  void changeTarget() throws SQLException {
    apply(shop(true));

    apply(schema(authors(), eventTable("order_events", 40), orders(text("2", "state", 20), "order_events", true)));
    insert(1, "NEW");

    assertThat(events("order_events")).containsExactly(Arrays.asList("1", "NEW", null));
  }

  @Test
  @DisplayName("Keeps the triggers working after a migration changes the type of a column")
  void changeType() throws SQLException {
    apply(shop(true));

    apply(schema(authors(), eventTable("order_events"), orders(text("2", "state", 40), "order_events", true)));
    insert(1, "PAID");

    assertThatThrownBy(() -> execute("DELETE FROM " + q("orders"))).isInstanceOf(SQLException.class)
        .hasMessageContaining("Paid orders stay.");
    assertThat(events("order_events")).hasSize(1);
  }

  @Test
  @DisplayName("Drops the triggers in a later migration")
  void dropTriggers() throws SQLException {
    apply(shop(true));

    apply(shop(false));
    insert(1, "PAID");
    execute("DELETE FROM " + q("orders"));

    assertThat(events("order_events")).isEmpty();
  }
}
