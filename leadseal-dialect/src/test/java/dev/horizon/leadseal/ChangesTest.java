package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.change.AddColumnAttributeChange;
import dev.horizon.leadseal.change.AddColumnChange;
import dev.horizon.leadseal.change.AddTableAttributeChange;
import dev.horizon.leadseal.change.AlterColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnTypeChange;
import dev.horizon.leadseal.change.AlterTableAttributeChange;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateTableChange;
import dev.horizon.leadseal.change.DropColumnAttributeChange;
import dev.horizon.leadseal.change.DropColumnChange;
import dev.horizon.leadseal.change.DropTableAttributeChange;
import dev.horizon.leadseal.change.DropTableChange;
import dev.horizon.leadseal.change.MoveTableChange;
import dev.horizon.leadseal.change.RenameColumnChange;
import dev.horizon.leadseal.change.RenameTableChange;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChangesTest {

  private static final Type TEXT_20 = new TextType(20);
  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64),
      NotNullAttribute.INSTANCE);
  private static final Column STATE = Column.of("2", "state", TEXT_20, NotNullAttribute.INSTANCE);
  private static final TableName ORDERS_NAME = TableName.of("orders");
  private static final Table ORDERS = orders(ID, STATE);

  private static Table orders(Column... columns) {
    return Table.of("shop.Order", "orders", List.of(columns), PrimaryKeyAttribute.of("orders", List.of("id")));
  }

  static Stream<Arguments> changes() {
    IndexAttribute index = IndexAttribute.of("orders", List.of("state"), false);
    PrimaryKeyAttribute composite = PrimaryKeyAttribute.of("orders", List.of("id", "state"));
    CheckAttribute positive = CheckAttribute.of("orders", "id > 0");
    CheckAttribute large = CheckAttribute.of("orders", "id > 1");
    Column note = Column.of("3", "note", new TextType(null));
    return Stream.of(
        arguments("create a table", Schema.empty(), Schema.of(ORDERS), List.of(new CreateTableChange(null, ORDERS))),
        arguments("drop a table", Schema.of(ORDERS), Schema.empty(), List.of(new DropTableChange(ORDERS_NAME))),
        arguments("rename a table by id", Schema.of(ORDERS),
            Schema.of(Table.of("shop.Order", "purchases", ORDERS.columns(), PrimaryKeyAttribute.of("purchases",
                List.of("id")))),
            List.of(new RenameTableChange(ORDERS_NAME, "purchases"))),
        arguments("move a table to another namespace", Schema.of(ORDERS),
            Schema.of(new Namespace("sales", List.of(ORDERS))), List.of(new MoveTableChange(ORDERS_NAME, "sales"))),
        arguments("rename a column by id", Schema.of(ORDERS), Schema.of(orders(ID, STATE.withName("status"))),
            List.of(new RenameColumnChange(ORDERS_NAME, "state", "status"))),
        arguments("change a type", Schema.of(ORDERS),
            Schema.of(orders(ID, Column.of("2", "state", new TextType(200), NotNullAttribute.INSTANCE))),
            List.of(new AlterColumnTypeChange(ORDERS_NAME, "state", TEXT_20, new TextType(200)))),
        arguments("make a column nullable", Schema.of(ORDERS),
            Schema.of(orders(ID, STATE.without(NotNullAttribute.class))),
            List.of(new DropColumnAttributeChange(ORDERS_NAME, "state", NotNullAttribute.INSTANCE))),
        arguments("add a default", Schema.of(ORDERS),
            Schema.of(orders(ID, STATE.with(new LiteralDefaultAttribute("new")))),
            List.of(new AddColumnAttributeChange(ORDERS_NAME, "state", new LiteralDefaultAttribute("new")))),
        arguments("change a default", Schema.of(orders(ID, STATE.with(new LiteralDefaultAttribute("new")))),
            Schema.of(orders(ID, STATE.with(new LiteralDefaultAttribute("open")))),
            List.of(new AlterColumnAttributeChange(ORDERS_NAME, "state", new LiteralDefaultAttribute("new"),
                new LiteralDefaultAttribute("open")))),
        arguments("add a column", Schema.of(ORDERS), Schema.of(orders(ID, STATE, note)),
            List.of(new AddColumnChange(ORDERS_NAME, note))),
        arguments("drop a column", Schema.of(ORDERS), Schema.of(orders(ID)),
            List.of(new DropColumnChange(ORDERS_NAME, "state"))),
        arguments("add an index", Schema.of(ORDERS), Schema.of(ORDERS.with(index)),
            List.of(new AddTableAttributeChange(ORDERS_NAME, index))),
        arguments("drop an index", Schema.of(ORDERS.with(index)), Schema.of(ORDERS),
            List.of(new DropTableAttributeChange(ORDERS_NAME, index))),
        arguments("change a primary key", Schema.of(ORDERS), Schema.of(ORDERS.with(composite)),
            List.of(new AlterTableAttributeChange(ORDERS_NAME, ORDERS.primaryKey(), composite))),
        arguments("comment a table", Schema.of(ORDERS), Schema.of(ORDERS.with(new CommentAttribute("Orders."))),
            List.of(new AddTableAttributeChange(ORDERS_NAME, new CommentAttribute("Orders.")))),
        arguments("add a check", Schema.of(ORDERS), Schema.of(ORDERS.with(positive)),
            List.of(new AddTableAttributeChange(ORDERS_NAME, positive))),
        arguments("change a check", Schema.of(ORDERS.with(positive)), Schema.of(ORDERS.with(large)),
            List.of(new DropTableAttributeChange(ORDERS_NAME, positive),
                new AddTableAttributeChange(ORDERS_NAME, large))),
        arguments("no change", Schema.of(ORDERS), Schema.of(ORDERS), List.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("changes")
  void findsTheChanges(String name, Schema before, Schema after, List<Change> expected) {
    assertThat(before.changesTo(after)).containsExactlyElementsOf(expected);
  }

  @Test
  void addsForeignKeysAfterAllTablesExist() {
    List<Change> changes = Schema.empty().changesTo(customersAndOrders());

    assertThat(changes).extracting(Object::getClass).containsExactly(CreateTableChange.class,
        CreateTableChange.class, AddTableAttributeChange.class);
  }

  @Test
  void dropsForeignKeysBeforeTheirTables() {
    List<Change> changes = customersAndOrders().changesTo(Schema.empty());

    assertThat(changes).extracting(Object::getClass).containsExactly(DropTableAttributeChange.class,
        DropTableChange.class, DropTableChange.class);
  }

  @Test
  void keepsAnIndexWhenItsColumnIsRenamed() {
    IndexAttribute index = IndexAttribute.of("orders", List.of("state"), false);
    Table after = orders(ID, STATE.withName("status"))
        .with(new IndexAttribute(index.name(), List.of("status"), false, List.of(), null));

    List<Change> changes = Schema.of(ORDERS.with(index)).changesTo(Schema.of(after));

    assertThat(changes).containsExactly(new RenameColumnChange(ORDERS_NAME, "state", "status"));
  }

  @Test
  void keepsAGeneratedPrimaryKeyNameWhenTheTableIsRenamed() {
    Table renamed = Table.of("shop.Order", "purchases", ORDERS.columns(), PrimaryKeyAttribute.of("purchases",
        List.of("id")));

    List<Change> changes = Schema.of(ORDERS).changesTo(Schema.of(renamed));

    assertThat(changes).containsExactly(new RenameTableChange(ORDERS_NAME, "purchases"));
  }

  @Test
  void ordersChangesByPhase() {
    Table before = ORDERS.with(IndexAttribute.of("orders", List.of("state"), false));
    Table after = Table.of("shop.Order", "purchases", List.of(ID, Column.of("3", "note", new TextType(null))),
        PrimaryKeyAttribute.of("purchases", List.of("id")));

    List<Change> changes = Schema.of(before).changesTo(Schema.of(after));

    assertThat(changes).extracting(Object::getClass).containsExactly(DropTableAttributeChange.class,
        RenameTableChange.class, AddColumnChange.class, DropColumnChange.class);
    assertThat(changes.getFirst().table()).isEqualTo(ORDERS_NAME);
    assertThat(changes.get(2).table()).isEqualTo(TableName.of("purchases"));
  }

  private static Schema customersAndOrders() {
    Table customers = Table.of("shop.Customer", "customers", List.of(ID), PrimaryKeyAttribute.of("customers",
        List.of("id")));
    Column customer = Column.of("2", "customer_id", ID.type(), NotNullAttribute.INSTANCE);
    Table orders = Table.of("shop.Order", "orders", List.of(ID, customer), PrimaryKeyAttribute.of("orders",
        List.of("id")),
        ForeignKeyAttribute.of("orders", List.of("customer_id"), TableName.of("customers"),
            List.of("id"), ForeignKeyAttribute.Action.CASCADE));
    return Schema.of(orders, customers);
  }
}
