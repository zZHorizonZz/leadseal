package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class EnumContractTest extends ContractTest {

  protected static final Column ID = column("1", "id", BIGINT);
  protected static final EnumType.Value NEW = new EnumType.Value(0, "NEW");
  protected static final EnumType.Value SENT = new EnumType.Value(1, "SENT");
  protected static final EnumType.Value LOST = new EnumType.Value(2, "LOST");
  protected static final EnumType.Value PACKED = new EnumType.Value(3, "PACKED");

  protected static EnumType names(EnumType.Value... values) {
    return new EnumType("shipment_state", List.of(values), EnumType.Storage.NAME);
  }

  protected static EnumType numbers(EnumType.Value... values) {
    return new EnumType("shipment_state", List.of(values), EnumType.Storage.NUMBER);
  }

  protected static Column state(Type type) {
    return column("2", "state", type);
  }

  protected static Table shipments(Column... columns) {
    return table("shop.Shipment", "shipments", Stream.concat(Stream.of(ID), Stream.of(columns)).toList(), "id");
  }

  protected void insert(String table, long id, Object state) throws SQLException {
    String value = state instanceof String name ? dialect.string(name) : String.valueOf(state);
    execute("INSERT INTO " + q(table) + " (" + q("id") + ", " + q("state") + ") VALUES (" + id + ", " + value + ")");
  }

  protected void insert(long id, Object state) throws SQLException {
    insert("shipments", id, state);
  }

  protected List<List<String>> states(String table) throws SQLException {
    return query("SELECT " + q("state") + " FROM " + q(table) + " ORDER BY " + q("id"));
  }

  @Test
  @DisplayName("Refuses a name that is not a value of the enum")
  void refuseName() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT)))));

    insert(1, "SENT");

    assertThatThrownBy(() -> insert(2, "LOST")).isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("Refuses a number that is not a value of the enum")
  void refuseNumber() throws SQLException {
    apply(schema(shipments(state(numbers(NEW, SENT)))));

    insert(1, 1);

    assertThatThrownBy(() -> insert(2, 5)).isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("Accepts a value that a later migration adds at the end")
  void appendValue() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT)))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, SENT, LOST)))));
    insert(2, "LOST");

    assertThat(states("shipments")).containsExactly(List.of("SENT"), List.of("LOST"));
  }

  @Test
  @DisplayName("Accepts a value that a later migration adds between two values")
  void insertValue() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT)))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, PACKED, SENT)))));
    insert(2, "PACKED");

    assertThat(states("shipments")).containsExactly(List.of("SENT"), List.of("PACKED"));
  }

  @Test
  @DisplayName("Refuses a value that a later migration removes, and keeps the rows")
  void removeValue() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT, LOST)))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, SENT)))));

    assertThatThrownBy(() -> insert(2, "LOST")).isInstanceOf(SQLException.class);
    assertThat(states("shipments")).containsExactly(List.of("SENT"));
  }

  @Test
  @DisplayName("Defaults to a value that the same migration adds")
  void defaultNewValue() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT)))));
    insert(1, "SENT");

    apply(schema(shipments(Column.of("2", "state", names(NEW, SENT, LOST), NotNull.INSTANCE,
        new LiteralDefault("LOST")))));
    execute("INSERT INTO " + q("shipments") + " (" + q("id") + ") VALUES (2)");

    assertThat(states("shipments")).containsExactly(List.of("SENT"), List.of("LOST"));
  }

  @Test
  @DisplayName("Changes the values of an enum that two tables share")
  void shareValues() throws SQLException {
    Table returns = table("shop.Return", "returns", List.of(ID, state(names(NEW, SENT, LOST))), "id");
    apply(schema(shipments(state(names(NEW, SENT, LOST))), returns));
    insert(1, "SENT");
    insert("returns", 1, "NEW");

    apply(schema(shipments(state(names(NEW, SENT))), returns.withColumns(List.of(ID, state(names(NEW, SENT))))));

    assertThatThrownBy(() -> insert("returns", 2, "LOST")).isInstanceOf(SQLException.class);
    assertThat(states("shipments")).containsExactly(List.of("SENT"));
    assertThat(states("returns")).containsExactly(List.of("NEW"));
  }

  @Test
  @DisplayName("Keeps the values of an enum through renames of the table and the column")
  void renameColumn() throws SQLException {
    apply(schema(shipments(state(numbers(NEW, SENT)))));
    insert(1, 1);

    apply(schema(table("shop.Shipment", "parcels", List.of(ID, column("2", "status", numbers(NEW, SENT))), "id")));
    apply(schema(table("shop.Shipment", "parcels", List.of(ID, column("2", "status", numbers(NEW, SENT, LOST))),
        "id")));

    execute("INSERT INTO " + q("parcels") + " (" + q("id") + ", " + q("status") + ") VALUES (2, 2)");
    assertThatThrownBy(() -> execute("INSERT INTO " + q("parcels") + " (" + q("id") + ", " + q("status")
        + ") VALUES (3, 5)")).isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("Drops an enum column")
  void dropColumn() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT))), table("shop.Return", "returns",
        List.of(ID, state(numbers(NEW, SENT))), "id")));

    apply(schema(shipments(), table("shop.Return", "returns", List.of(ID), "id")));

    assertThat(columns("shipments")).containsOnlyKeys("id");
    assertThat(columns("returns")).containsOnlyKeys("id");
  }

  @Test
  @DisplayName("Changes a text column to an enum, and back")
  void changeType() throws SQLException {
    apply(schema(shipments(state(TEXT))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, SENT)))));
    assertThatThrownBy(() -> insert(2, "LOST")).isInstanceOf(SQLException.class);

    apply(schema(shipments(state(TEXT))));
    insert(3, "LOST");
    assertThat(states("shipments")).containsExactly(List.of("SENT"), List.of("LOST"));
  }

  @Test
  @DisplayName("Makes an enum column required")
  void requireValue() throws SQLException {
    apply(schema(shipments(nullable("2", "state", names(NEW, SENT)))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, SENT)))));

    assertThat(columns("shipments")).containsEntry("state", false);
    assertThatThrownBy(() -> insert(2, "LOST")).isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("Renames a value of an enum that stores numbers")
  void renameNumber() throws SQLException {
    apply(schema(shipments(state(numbers(NEW, SENT)))));
    insert(1, 1);

    apply(schema(shipments(state(numbers(NEW, new EnumType.Value(1, "SHIPPED"))))));

    assertThat(states("shipments")).containsExactly(List.of("1"));
  }
}
