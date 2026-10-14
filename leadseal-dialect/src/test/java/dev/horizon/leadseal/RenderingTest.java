package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RenderingTest {

  private static final Dialect DIALECT = new StandardDialect();
  private static final Type BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final Column ID = Column.of("1", "id", BIGINT, NotNullAttribute.INSTANCE);
  private static final Column STATE = Column.of("2", "state", new TextType(20), NotNullAttribute.INSTANCE);

  private static Table orders(String name, Column... columns) {
    return Table.of("shop.Order", name, List.of(columns), PrimaryKeyAttribute.of(name, List.of("id")),
        IndexAttribute.of(name, List.of(columns[1].name()), false));
  }

  static Stream<Arguments> migrations() {
    Table orders = orders("orders", ID, STATE);
    return Stream.of(
        arguments("create table", Schema.empty(), Schema.of(orders), List.of(
            "CREATE TABLE \"orders\" (\"id\" INT64 NOT NULL, \"state\" TEXT(20) NOT NULL,"
                + " CONSTRAINT \"pk_orders\" PRIMARY KEY (\"id\"))",
            "CREATE INDEX \"ix_orders_state\" ON \"orders\" (\"state\")")),
        arguments("add a not null column", Schema.of(orders), Schema.of(orders("orders", ID, STATE, Column.of("3",
            "total", new IntegerType(IntegerType.Size.INT32), NotNullAttribute.INSTANCE))), List.of(
                "ALTER TABLE \"orders\" ADD COLUMN \"total\" INT32 NOT NULL DEFAULT 0",
                "ALTER TABLE \"orders\" ALTER COLUMN \"total\" DROP DEFAULT")),
        arguments("rename a table and a column", Schema.of(orders),
            Schema.of(orders("purchases", ID, STATE.withName("status"))), List.of(
                "DROP INDEX \"ix_orders_state\"",
                "ALTER TABLE \"orders\" RENAME TO \"purchases\"",
                "ALTER TABLE \"purchases\" RENAME CONSTRAINT \"pk_orders\" TO \"pk_purchases\"",
                "ALTER TABLE \"purchases\" RENAME COLUMN \"state\" TO \"status\"",
                "CREATE INDEX \"ix_purchases_status\" ON \"purchases\" (\"status\")")),
        arguments("make a column nullable", Schema.of(orders),
            Schema.of(orders("orders", ID, STATE.without(NotNullAttribute.class))), List.of(
                "ALTER TABLE \"orders\" ALTER COLUMN \"state\" DROP NOT NULL")),
        arguments("make a column required and longer",
            Schema.of(orders("orders", ID, STATE.without(NotNullAttribute.class))),
            Schema.of(orders("orders", ID, Column.of("2", "state", new TextType(40), NotNullAttribute.INSTANCE))),
            List.of(
                "ALTER TABLE \"orders\" ALTER COLUMN \"state\" TYPE TEXT(40) USING \"state\"::TEXT(40)",
                "UPDATE \"orders\" SET \"state\" = '' WHERE \"state\" IS NULL",
                "ALTER TABLE \"orders\" ALTER COLUMN \"state\" SET NOT NULL")),
        arguments("move a table", Schema.of(orders), Schema.of(new Namespace("sales", List.of(orders))), List.of(
            "CREATE SCHEMA IF NOT EXISTS \"sales\"",
            "ALTER TABLE \"orders\" SET SCHEMA \"sales\"")),
        arguments("drop a table", Schema.of(orders), Schema.empty(), List.of("DROP TABLE \"orders\"")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("migrations")
  void rendersStandardSql(String name, Schema before, Schema after, List<String> statements) {
    ScriptWriter script = new ScriptWriter(DIALECT, before, after).write(before.changesTo(after));

    assertThat(script.statements()).containsExactlyElementsOf(statements);
  }

  @Test
  void rendersForeignKeys() {
    Table customers = Table.of("shop.Customer", "customers", List.of(ID), PrimaryKeyAttribute.of("customers",
        List.of("id")));
    Table orders = Table.of("shop.Order", "orders", List.of(ID, Column.of("2", "customer_id", BIGINT)),
        PrimaryKeyAttribute.of("orders", List.of("id")), ForeignKeyAttribute.of("orders", List.of("customer_id"),
            TableName.of("customers"), List.of("id"), ForeignKeyAttribute.Action.SET_NULL));
    Schema after = Schema.of(customers, orders);

    ScriptWriter script = new ScriptWriter(DIALECT, Schema.empty(), after)
        .write(Schema.empty().changesTo(after));

    assertThat(script.statements()).last().isEqualTo("ALTER TABLE \"orders\" ADD CONSTRAINT"
        + " \"fk_orders_customer_id\" FOREIGN KEY (\"customer_id\") REFERENCES \"customers\" (\"id\")"
        + " ON DELETE SET NULL");
  }

  @Test
  void writesStatementsWithSemicolons() {
    ScriptWriter script = new ScriptWriter(DIALECT, Schema.empty(), Schema.empty());

    assertThat(script.text()).isEmpty();
    assertThat(script.append("A").end().append("B").end().text()).isEqualTo("A;\n\nB;\n");
  }
}
