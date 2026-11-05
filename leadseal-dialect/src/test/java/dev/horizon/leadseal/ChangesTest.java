package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.change.AddColumn;
import dev.horizon.leadseal.change.AddColumnAttribute;
import dev.horizon.leadseal.change.AddTableAttribute;
import dev.horizon.leadseal.change.AlterColumnAttribute;
import dev.horizon.leadseal.change.AlterColumnType;
import dev.horizon.leadseal.change.AlterTableAttribute;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateTable;
import dev.horizon.leadseal.change.DropColumn;
import dev.horizon.leadseal.change.DropColumnAttribute;
import dev.horizon.leadseal.change.DropTable;
import dev.horizon.leadseal.change.DropTableAttribute;
import dev.horizon.leadseal.change.MoveTable;
import dev.horizon.leadseal.change.RenameColumn;
import dev.horizon.leadseal.change.RenameTable;
import dev.horizon.leadseal.change.TableChange;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChangesTest {

  private static final Type TEXT_20 = TextType.of(20);
  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64),
      NotNull.INSTANCE);
  private static final Column STATE = Column.of("2", "state", TEXT_20, NotNull.INSTANCE);
  private static final TableName ORDERS_NAME = TableName.of("orders");
  private static final Table ORDERS = orders(ID, STATE);

  private static Table orders(Column... columns) {
    return Table.of("shop.Order", "orders", List.of(columns), PrimaryKey.of("orders", List.of("id")));
  }

  static Stream<Arguments> changes() {
    Index index = Index.of("orders", List.of("state"), false);
    PrimaryKey composite = PrimaryKey.of("orders", List.of("id", "state"));
    Check positive = Check.of("orders", "id > 0");
    Check large = Check.of("orders", "id > 1");
    Column note = Column.of("3", "note", TextType.unlimited());
    return Stream.of(
        arguments("create a table", Schema.empty(), Schema.of(ORDERS),
            List.of(new CreateTable(Namespace.Name.DEFAULT, ORDERS))),
        arguments("drop a table", Schema.of(ORDERS), Schema.empty(), List.of(new DropTable(ORDERS_NAME))),
        arguments("rename a table by id", Schema.of(ORDERS),
            Schema.of(Table.of("shop.Order", "purchases", ORDERS.columns(), PrimaryKey.of("purchases",
                List.of("id")))),
            List.of(new RenameTable(ORDERS_NAME, "purchases"))),
        arguments("move a table to another namespace", Schema.of(ORDERS),
            Schema.of(new Namespace("sales", List.of(ORDERS))),
            List.of(new MoveTable(ORDERS_NAME, Namespace.Name.of("sales")))),
        arguments("rename a column by id", Schema.of(ORDERS), Schema.of(orders(ID, STATE.withName("status"))),
            List.of(new RenameColumn(ORDERS_NAME, "state", "status"))),
        arguments("change a type", Schema.of(ORDERS),
            Schema.of(orders(ID, Column.of("2", "state", TextType.of(200), NotNull.INSTANCE))),
            List.of(new AlterColumnType(ORDERS_NAME, "state", TEXT_20, TextType.of(200)))),
        arguments("make a column nullable", Schema.of(ORDERS),
            Schema.of(orders(ID, STATE.without(NotNull.class))),
            List.of(new DropColumnAttribute(ORDERS_NAME, "state", NotNull.INSTANCE))),
        arguments("add a default", Schema.of(ORDERS),
            Schema.of(orders(ID, STATE.with(new LiteralDefault("new")))),
            List.of(new AddColumnAttribute(ORDERS_NAME, "state", new LiteralDefault("new")))),
        arguments("change a default", Schema.of(orders(ID, STATE.with(new LiteralDefault("new")))),
            Schema.of(orders(ID, STATE.with(new LiteralDefault("open")))),
            List.of(new AlterColumnAttribute(ORDERS_NAME, "state", new LiteralDefault("new"),
                new LiteralDefault("open")))),
        arguments("add a column", Schema.of(ORDERS), Schema.of(orders(ID, STATE, note)),
            List.of(new AddColumn(ORDERS_NAME, note))),
        arguments("drop a column", Schema.of(ORDERS), Schema.of(orders(ID)),
            List.of(new DropColumn(ORDERS_NAME, "state"))),
        arguments("add an index", Schema.of(ORDERS), Schema.of(ORDERS.with(index)),
            List.of(new AddTableAttribute(ORDERS_NAME, index))),
        arguments("drop an index", Schema.of(ORDERS.with(index)), Schema.of(ORDERS),
            List.of(new DropTableAttribute(ORDERS_NAME, index))),
        arguments("change a primary key", Schema.of(ORDERS), Schema.of(ORDERS.with(composite)),
            List.of(new AlterTableAttribute(ORDERS_NAME, ORDERS.primaryKey(), composite))),
        arguments("comment a table", Schema.of(ORDERS), Schema.of(ORDERS.with(new Comment("Orders."))),
            List.of(new AddTableAttribute(ORDERS_NAME, new Comment("Orders.")))),
        arguments("add a check", Schema.of(ORDERS), Schema.of(ORDERS.with(positive)),
            List.of(new AddTableAttribute(ORDERS_NAME, positive))),
        arguments("change a check", Schema.of(ORDERS.with(positive)), Schema.of(ORDERS.with(large)),
            List.of(new DropTableAttribute(ORDERS_NAME, positive),
                new AddTableAttribute(ORDERS_NAME, large))),
        arguments("no change", Schema.of(ORDERS), Schema.of(ORDERS), List.of()));
  }

  @ParameterizedTest(name = "({0})", quoteTextArguments = false)
  @MethodSource("changes")
  @DisplayName("Finds the changes")
  void findChanges(String name, Schema before, Schema after, List<Change> expected) {
    assertThat(before.changesTo(after)).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("Adds foreign keys after all tables exist")
  void addForeignKeysLast() {
    List<Change> changes = Schema.empty().changesTo(customersAndOrders());

    assertThat(changes).extracting(Object::getClass).containsExactly(CreateTable.class,
        CreateTable.class, AddTableAttribute.class);
  }

  @Test
  @DisplayName("Drops foreign keys before their tables")
  void dropForeignKeysFirst() {
    List<Change> changes = customersAndOrders().changesTo(Schema.empty());

    assertThat(changes).extracting(Object::getClass).containsExactly(DropTableAttribute.class,
        DropTable.class, DropTable.class);
  }

  @Test
  @DisplayName("Keeps an index when its column is renamed")
  void keepIndexOnRename() {
    Index index = Index.of("orders", List.of("state"), false);
    Table after = orders(ID, STATE.withName("status"))
        .with(new Index(index.name(), List.of("status"), false, List.of(), null));

    List<Change> changes = Schema.of(ORDERS.with(index)).changesTo(Schema.of(after));

    assertThat(changes).containsExactly(new RenameColumn(ORDERS_NAME, "state", "status"));
  }

  @Test
  @DisplayName("Keeps a generated primary key name when the table is renamed")
  void keepPrimaryKeyName() {
    Table renamed = Table.of("shop.Order", "purchases", ORDERS.columns(), PrimaryKey.of("purchases",
        List.of("id")));

    List<Change> changes = Schema.of(ORDERS).changesTo(Schema.of(renamed));

    assertThat(changes).containsExactly(new RenameTable(ORDERS_NAME, "purchases"));
  }

  @Test
  @DisplayName("Orders changes by phase")
  void orderByPhase() {
    Table before = ORDERS.with(Index.of("orders", List.of("state"), false));
    Table after = Table.of("shop.Order", "purchases", List.of(ID, Column.of("3", "note", TextType.unlimited())),
        PrimaryKey.of("purchases", List.of("id")));

    List<Change> changes = Schema.of(before).changesTo(Schema.of(after));

    assertThat(changes).extracting(Object::getClass).containsExactly(DropTableAttribute.class,
        RenameTable.class, AddColumn.class, DropColumn.class);
    assertThat(((TableChange) changes.getFirst()).table()).isEqualTo(ORDERS_NAME);
    assertThat(((TableChange) changes.get(2)).table()).isEqualTo(TableName.of("purchases"));
  }

  private static Schema customersAndOrders() {
    Table customers = Table.of("shop.Customer", "customers", List.of(ID), PrimaryKey.of("customers",
        List.of("id")));
    Column customer = Column.of("2", "customer_id", ID.type(), NotNull.INSTANCE);
    Table orders = Table.of("shop.Order", "orders", List.of(ID, customer), PrimaryKey.of("orders",
        List.of("id")),
        ForeignKey.of("orders", List.of("customer_id"), TableName.of("customers"),
            List.of("id"), ForeignKey.Action.CASCADE));
    return Schema.of(orders, customers);
  }
}
