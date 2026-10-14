package dev.horizon.leadseal;

import static dev.horizon.leadseal.Fixtures.column;
import static dev.horizon.leadseal.Fixtures.nullable;
import static dev.horizon.leadseal.Fixtures.schema;
import static dev.horizon.leadseal.Fixtures.table;
import static dev.horizon.leadseal.Fixtures.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.Operation.AddColumn;
import dev.horizon.leadseal.Operation.AddForeignKey;
import dev.horizon.leadseal.Operation.AddIndex;
import dev.horizon.leadseal.Operation.AlterColumn;
import dev.horizon.leadseal.Operation.AlterPrimaryKey;
import dev.horizon.leadseal.Operation.CreateTable;
import dev.horizon.leadseal.Operation.DropColumn;
import dev.horizon.leadseal.Operation.DropForeignKey;
import dev.horizon.leadseal.Operation.DropIndex;
import dev.horizon.leadseal.Operation.DropTable;
import dev.horizon.leadseal.Operation.RenameColumn;
import dev.horizon.leadseal.Operation.RenameTable;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DiffTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Column STATE = text("2", "state", 20);
  private static final Table ORDERS = table("shop.Order", "orders").column(ID).column(STATE).primaryKey("id").build();
  private static final Table CUSTOMERS = table("shop.Customer", "customers").column(ID).primaryKey("id").build();

  static Stream<Arguments> changes() {
    Table renamedState = table("shop.Order", "orders").column(ID).column(text("2", "status", 20)).primaryKey("id")
        .build();
    Table longerState = table("shop.Order", "orders").column(ID).column(text("2", "state", 200)).primaryKey("id")
        .build();
    Table optionalState = table("shop.Order", "orders").column(ID).column(new Column("2", "state", ColumnType.TEXT,
        20, true)).primaryKey("id").build();
    Table withNote = table("shop.Order", "orders").column(ID).column(STATE).column(nullable("3", "note",
        ColumnType.TEXT)).primaryKey("id").build();
    Table withoutState = table("shop.Order", "orders").column(ID).primaryKey("id").build();
    Table renamedTable = table("shop.Order", "purchases").column(ID).column(STATE).primaryKey("id").build();
    Table indexed = table("shop.Order", "orders").column(ID).column(STATE).primaryKey("id").index(false, "state")
        .build();
    Table compositeKey = table("shop.Order", "orders").column(ID).column(STATE).primaryKey("id", "state").build();
    return Stream.of(
        arguments("create a table", schema(), schema(ORDERS), List.of(new CreateTable(ORDERS))),
        arguments("drop a table", schema(ORDERS), schema(), List.of(new DropTable("orders"))),
        arguments("rename a table by id", schema(ORDERS), schema(renamedTable),
            List.of(new RenameTable("orders", "purchases"))),
        arguments("rename a column by id", schema(ORDERS), schema(renamedState),
            List.of(new RenameColumn("orders", "state", "status"))),
        arguments("change a length", schema(ORDERS), schema(longerState),
            List.of(new AlterColumn("orders", STATE, text("2", "state", 200)))),
        arguments("make a column nullable", schema(ORDERS), schema(optionalState),
            List.of(new AlterColumn("orders", STATE, optionalState.columns().get(1)))),
        arguments("add a column", schema(ORDERS), schema(withNote),
            List.of(new AddColumn("orders", withNote.columns().get(2)))),
        arguments("drop a column", schema(ORDERS), schema(withoutState), List.of(new DropColumn("orders", "state"))),
        arguments("add an index", schema(ORDERS), schema(indexed),
            List.of(new AddIndex("orders", indexed.indexes().getFirst()))),
        arguments("drop an index", schema(indexed), schema(ORDERS),
            List.of(new DropIndex("orders", indexed.indexes().getFirst()))),
        arguments("change a primary key", schema(ORDERS), schema(compositeKey),
            List.of(new AlterPrimaryKey("orders", ORDERS.primaryKey(), compositeKey.primaryKey()))),
        arguments("no change", schema(ORDERS), schema(ORDERS), List.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("changes")
  void findsTheOperations(String name, Schema before, Schema after, List<Operation> expected) {
    assertThat(new Diff(before, after).operations()).containsExactlyElementsOf(expected);
  }

  @Test
  void addsForeignKeysAfterAllTablesExist() {
    Table orders = table("shop.Order", "orders").column(ID).column(column("2", "customer_id", ColumnType.BIGINT))
        .primaryKey("id").foreignKey("customer_id", "customers", "id", ForeignKey.Action.CASCADE).build();

    List<Operation> operations = new Diff(schema(), schema(orders, CUSTOMERS)).operations();

    assertThat(operations).extracting(Object::getClass).containsExactly(CreateTable.class, CreateTable.class,
        AddForeignKey.class);
  }

  @Test
  void dropsForeignKeysBeforeTheirTables() {
    Table orders = table("shop.Order", "orders").column(ID).column(column("2", "customer_id", ColumnType.BIGINT))
        .primaryKey("id").foreignKey("customer_id", "customers", "id", ForeignKey.Action.CASCADE).build();

    List<Operation> operations = new Diff(schema(orders, CUSTOMERS), schema()).operations();

    assertThat(operations).extracting(Object::getClass).containsExactly(DropForeignKey.class, DropTable.class,
        DropTable.class);
  }

  @Test
  void keepsAnIndexWhenItsColumnIsRenamed() {
    Table before = table("shop.Order", "orders").column(ID).column(STATE).primaryKey("id").build();
    Table after = new Table("shop.Order", "orders", List.of(ID, text("2", "status", 20)), before.primaryKey(),
        List.of(), List.of());
    Index index = Index.of("orders", List.of("state"), false);
    Table indexedBefore = new Table(before.id(), before.name(), before.columns(), before.primaryKey(),
        List.of(index), List.of());
    Table indexedAfter = new Table(after.id(), after.name(), after.columns(), after.primaryKey(),
        List.of(new Index(index.name(), List.of("status"), false)), List.of());

    List<Operation> operations = new Diff(schema(indexedBefore), schema(indexedAfter)).operations();

    assertThat(operations).containsExactly(new RenameColumn("orders", "state", "status"));
  }

  @Test
  void ordersOperationsByKind() {
    Table before = table("shop.Order", "orders").column(ID).column(STATE).primaryKey("id").index(false, "state")
        .build();
    Table after = table("shop.Order", "purchases").column(ID).column(nullable("3", "note", ColumnType.TEXT))
        .primaryKey("id").build();

    List<Operation> operations = new Diff(schema(before), schema(after)).operations();

    assertThat(operations).extracting(Object::getClass).containsExactly(DropIndex.class, RenameTable.class,
        AddColumn.class, DropColumn.class);
    assertThat(operations.getFirst().table()).isEqualTo("orders");
    assertThat(operations.get(2).table()).isEqualTo("purchases");
  }
}
