package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Generated;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.change.AddColumn;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.DropColumn;
import dev.horizon.leadseal.change.DropGeneratedColumn;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.ForeignColumn;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeneratedTest {

  private static final IntegerType BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final TableName ORDERS = TableName.of("orders");
  private static final Column ID = Column.of("1", "id", BIGINT, NotNull.INSTANCE);
  private static final Column PRICE = Column.of("2", "price", BIGINT, NotNull.INSTANCE);
  private static final Column QUANTITY = Column.of("3", "quantity", BIGINT, NotNull.INSTANCE);
  private static final SqlExpression PRODUCT = new Arithmetic(Arithmetic.Operator.MULTIPLY, ColumnReference.of(
      "price"), ColumnReference.of("quantity"));
  private static final Column TOTAL = Column.of("4", "total", BIGINT, new Generated(PRODUCT));

  private static Table orders(List<Column> columns, TableAttribute... attributes) {
    List<TableAttribute> all = new ArrayList<>(List.of(PrimaryKey.of("orders", List.of("id"))));
    all.addAll(List.of(attributes));
    return new Table("shop.Order", "orders", columns, all);
  }

  private static Table orders(Column... columns) {
    return orders(List.of(columns));
  }

  private static Column total(SqlExpression expression) {
    return Column.of("4", "total", BIGINT, new Generated(expression));
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        arguments("a generated column that does not accept NULL", (Executable) () -> Column.of("4", "total", BIGINT,
            NotNull.INSTANCE, new Generated(PRODUCT)), "must accept NULL"),
        arguments("a generated column with a default", (Executable) () -> Column.of("4", "total", BIGINT,
            new LiteralDefault("0"), new Generated(PRODUCT)), "no default"),
        arguments("a generated column with a collation", (Executable) () -> Column.of("4", "total", TextType.unlimited(),
            new Collation("postgresql", "C"), new Generated(ColumnReference.of("price"))), "no collation"),
        arguments("a generated column that calls now()", (Executable) () -> total(new Call(Call.Function.NOW,
            List.of())), "calls now() or uuid()"),
        arguments("a generated column that reads another table", (Executable) () -> total(new ForeignColumn(
            ColumnReference.of("price"), TableName.of("prices"), "id", "amount")), "reads another table"),
        arguments("a generated column that reads a column the table does not have", (Executable) () -> orders(ID, PRICE,
            total(ColumnReference.of("missing"))), "missing"),
        arguments("a generated column that reads a generated column", (Executable) () -> orders(ID, PRICE, QUANTITY,
            TOTAL, Column.of("5", "twice", BIGINT, new Generated(new Arithmetic(Arithmetic.Operator.ADD,
                ColumnReference.of("total"), ColumnReference.of("total"))))),
            "reads the generated column total"),
        arguments("a generated column in the primary key", (Executable) () -> new Table("shop.Order", "orders", List.of(
            PRICE, QUANTITY, TOTAL), List.of(PrimaryKey.of("orders", List.of("total")))), "must not accept NULL"),
        arguments("a generated column in an index", (Executable) () -> orders(List.of(ID, PRICE, QUANTITY, TOTAL),
            Index.of("orders", List.of("total"), false)), "in a key or an index"),
        arguments("a generated column in a foreign key", (Executable) () -> orders(List.of(ID, PRICE, QUANTITY, TOTAL),
            ForeignKey.of("orders", List.of("total"), TableName.of("totals"), List.of("id"),
                ForeignKey.Action.NO_ACTION)),
            "in a key or an index"),
        arguments("a rewrite of a generated column", (Executable) () -> orders(ID, PRICE, QUANTITY, TOTAL).with(
            Triggers.of(new Rewrite("total", ColumnReference.of("price")))), "which is a generated column"),
        arguments("a rewrite that reads a generated column", (Executable) () -> orders(ID, PRICE, QUANTITY, TOTAL)
            .with(Triggers.of(new Rewrite("price", ColumnReference.of("total")))), "reads the generated column total"),
        arguments("a trigger that writes a generated column", (Executable) () -> Schema.of(new Table("shop.Log", "logs",
            List.of(ID), List.of(PrimaryKey.of("logs", List.of("id")))).with(
                new Triggers(List.of(), List.of(
                    new Trigger("copy", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Insert(ORDERS,
                        List.of(new Trigger.Assignment("id", ColumnReference.of("id")), new Trigger.Assignment("price",
                            ColumnReference.of("id")), new Trigger.Assignment("quantity", ColumnReference.of("id")),
                            new Trigger.Assignment("total", ColumnReference.of("id"))))))))),
            orders(ID, PRICE, QUANTITY,
                TOTAL)),
            "which is a generated column"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName("Refuses a generated column that the databases cannot keep")
  void refuse(String name, Executable definition, String message) {
    assertThatThrownBy(definition::execute).isInstanceOf(SchemaException.class).hasMessageContaining(message);
  }

  @Test
  @DisplayName("Accepts a generated column that reads the columns of its row")
  void accept() {
    assertThat(orders(ID, PRICE, QUANTITY, TOTAL).column("total").flatMap(column -> column.attribute(Generated.class)))
        .hasValue(new Generated(PRODUCT));
  }

  private List<Change> changes(Table origin, Table target) {
    return Schema.of(origin).changesTo(Schema.of(target));
  }

  @Test
  @DisplayName("Makes no change for a generated column that stays the same")
  void keep() {
    assertThat(changes(orders(ID, PRICE, QUANTITY, TOTAL), orders(ID, PRICE, QUANTITY, TOTAL))).isEmpty();
  }

  @Test
  @DisplayName("Adds a generated column after the columns that it reads")
  void add() {
    List<Change> changes = changes(orders(ID, PRICE), orders(ID, PRICE, QUANTITY, TOTAL));

    assertThat(changes).containsExactly(new AddColumn(ORDERS, QUANTITY), new AddColumn(ORDERS, TOTAL));
    assertThat(changes.getLast().phase()).isEqualTo(Change.Phase.ADD_GENERATED_COLUMNS);
    assertThat(changes.getFirst().phase().precedes(changes.getLast().phase())).isTrue();
  }

  @Test
  @DisplayName("Drops a generated column before the changes of the columns that it read, with no warning")
  void drop() {
    List<Change> changes = changes(orders(ID, PRICE, QUANTITY, TOTAL), orders(ID, PRICE));

    assertThat(changes).contains(new DropGeneratedColumn(ORDERS, "total"), new DropColumn(ORDERS, "quantity"));
    assertThat(new DropGeneratedColumn(ORDERS, "total").phase().precedes(Change.Phase.RENAME_COLUMNS)).isTrue();
    assertThat(new DropGeneratedColumn(ORDERS, "total").warning()).isEmpty();
  }

  @Test
  @DisplayName("Drops a generated column and adds it again when its expression changes")
  void changeExpression() {
    Column plusOne = total(new Arithmetic(Arithmetic.Operator.ADD, PRODUCT, ColumnReference.of("price")));

    assertThat(changes(orders(ID, PRICE, QUANTITY, TOTAL), orders(ID, PRICE, QUANTITY, plusOne))).containsExactly(
        new DropGeneratedColumn(ORDERS, "total"), new AddColumn(ORDERS, plusOne));
  }

  @Test
  @DisplayName("Drops a generated column and adds it again when a column that it reads is renamed or changes type")
  void changeRead() {
    Column amount = Column.of("3", "amount", BIGINT, NotNull.INSTANCE);
    Column renamed = total(new Arithmetic(Arithmetic.Operator.MULTIPLY, ColumnReference.of("price"), ColumnReference
        .of("amount")));
    Column wider = Column.of("3", "quantity", TextType.unlimited(), NotNull.INSTANCE);

    assertThat(changes(orders(ID, PRICE, QUANTITY, TOTAL), orders(ID, PRICE, amount, renamed))).contains(
        new DropGeneratedColumn(ORDERS, "total"), new AddColumn(ORDERS, renamed));
    assertThat(changes(orders(ID, PRICE, QUANTITY, TOTAL), orders(ID, PRICE, wider, TOTAL))).contains(
        new DropGeneratedColumn(ORDERS, "total"), new AddColumn(ORDERS, TOTAL));
  }

  @Test
  @DisplayName("Replaces a column that becomes generated, with a warning that its values go")
  void replaceColumn() {
    Column plain = Column.of("4", "total", BIGINT);

    List<Change> changes = changes(orders(ID, PRICE, QUANTITY, plain), orders(ID, PRICE, QUANTITY, TOTAL));

    assertThat(changes).containsExactly(new DropColumn(ORDERS, "total"), new AddColumn(ORDERS, TOTAL));
    assertThat(changes.getFirst().warning()).isPresent();
  }

  @Test
  @DisplayName("Refuses to make a generated column a column that is not generated")
  void refusePlain() {
    Schema origin = Schema.of(orders(ID, PRICE, QUANTITY, TOTAL));
    Schema target = Schema.of(orders(ID, PRICE, QUANTITY, Column.of("4", "total", BIGINT)));

    assertThatThrownBy(() -> origin.changesTo(target)).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("a column that is not generated");
  }
}
