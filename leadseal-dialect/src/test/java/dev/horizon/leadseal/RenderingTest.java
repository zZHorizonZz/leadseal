package dev.horizon.leadseal;

import static dev.horizon.leadseal.Fixtures.column;
import static dev.horizon.leadseal.Fixtures.nullable;
import static dev.horizon.leadseal.Fixtures.schema;
import static dev.horizon.leadseal.Fixtures.table;
import static dev.horizon.leadseal.Fixtures.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RenderingTest {

  private static final Dialect DIALECT = new StandardDialect();
  private static final Column ID = column("1", "id", ColumnType.BIGINT);
  private static final Table ORDERS = table("shop.Order", "orders").column(ID).column(text("2", "state", 20))
      .primaryKey("id").index(false, "state").build();

  static Stream<Arguments> migrations() {
    Table withNote = table("shop.Order", "orders").column(ID).column(text("2", "state", 20))
        .column(column("3", "total", ColumnType.INT)).primaryKey("id").index(false, "state").build();
    Table renamed = table("shop.Order", "purchases").column(ID).column(text("2", "status", 20)).primaryKey("id")
        .index(false, "status").build();
    Table optional = table("shop.Order", "orders").column(ID).column(new Column("2", "state", ColumnType.TEXT, 20,
        true)).primaryKey("id").index(false, "state").build();
    Table required = table("shop.Order", "orders").column(ID).column(text("2", "state", 40)).primaryKey("id")
        .index(false, "state").build();
    return Stream.of(
        arguments("create table", schema(), schema(ORDERS), List.of(
            "CREATE TABLE \"orders\" (\"id\" BIGINT NOT NULL, \"state\" TEXT(20) NOT NULL,"
                + " CONSTRAINT \"pk_orders\" PRIMARY KEY (\"id\"))",
            "CREATE INDEX \"ix_orders_state\" ON \"orders\" (\"state\")")),
        arguments("add a not null column", schema(ORDERS), schema(withNote), List.of(
            "ALTER TABLE \"orders\" ADD COLUMN \"total\" INT NOT NULL DEFAULT 0",
            "ALTER TABLE \"orders\" ALTER COLUMN \"total\" DROP DEFAULT")),
        arguments("rename a table and a column", schema(ORDERS), schema(renamed), List.of(
            "DROP INDEX \"ix_orders_state\"",
            "ALTER TABLE \"orders\" RENAME TO \"purchases\"",
            "ALTER TABLE \"purchases\" RENAME CONSTRAINT \"pk_orders\" TO \"pk_purchases\"",
            "ALTER TABLE \"purchases\" RENAME COLUMN \"state\" TO \"status\"",
            "CREATE INDEX \"ix_purchases_status\" ON \"purchases\" (\"status\")")),
        arguments("make a column nullable", schema(ORDERS), schema(optional), List.of(
            "ALTER TABLE \"orders\" ALTER COLUMN \"state\" DROP NOT NULL")),
        arguments("make a column required and longer", schema(optional), schema(required), List.of(
            "ALTER TABLE \"orders\" ALTER COLUMN \"state\" TYPE TEXT(40) USING \"state\"::TEXT(40)",
            "UPDATE \"orders\" SET \"state\" = '' WHERE \"state\" IS NULL",
            "ALTER TABLE \"orders\" ALTER COLUMN \"state\" SET NOT NULL")),
        arguments("drop a table", schema(ORDERS), schema(), List.of("DROP TABLE \"orders\"")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("migrations")
  void rendersStandardSql(String name, Schema before, Schema after, List<String> statements) {
    SqlScript script = DdlWriter.script(DIALECT, new Diff(before, after).operations(), before, after);

    assertThat(script.statements()).containsExactlyElementsOf(statements);
  }

  @Test
  void rendersForeignKeys() {
    Table customers = table("shop.Customer", "customers").column(ID).primaryKey("id").build();
    Table orders = table("shop.Order", "orders").column(ID).column(nullable("2", "customer_id", ColumnType.BIGINT))
        .primaryKey("id").foreignKey("customer_id", "customers", "id", ForeignKey.Action.SET_NULL).build();

    SqlScript script = DdlWriter.script(DIALECT, new Diff(schema(), schema(customers, orders)).operations(),
        schema(), schema(customers, orders));

    assertThat(script.statements()).last().isEqualTo("ALTER TABLE \"orders\" ADD CONSTRAINT"
        + " \"fk_orders_customer_id\" FOREIGN KEY (\"customer_id\") REFERENCES \"customers\" (\"id\")"
        + " ON DELETE SET NULL");
  }

  @Test
  void writesStatementsWithSemicolons() {
    assertThat(new SqlScript(List.of("A", "B")).text()).isEqualTo("A;\n\nB;\n");
    assertThat(new SqlScript(List.of()).text()).isEmpty();
  }
}
