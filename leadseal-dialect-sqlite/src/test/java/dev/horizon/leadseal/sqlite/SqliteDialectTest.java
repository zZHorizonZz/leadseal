package dev.horizon.leadseal.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.TextType;
import dev.horizon.leadseal.attribute.CommentAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqliteDialectTest {

  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64),
      NotNullAttribute.INSTANCE);
  private static final Table ORDERS = Table.of("shop.Order", "orders", List.of(ID, Column.of("2", "customer_id",
      ID.type(), NotNullAttribute.INSTANCE)), PrimaryKeyAttribute.of("orders", List.of("id")),
      ForeignKeyAttribute.of("orders", List.of("customer_id"), TableName.of("customers"), List.of("id"),
          ForeignKeyAttribute.Action.CASCADE));

  private static Schema customers(int length) {
    return Schema.of(Table.of("shop.Customer", "customers", List.of(ID, Column.of("2", "name", new TextType(length),
        NotNullAttribute.INSTANCE)), PrimaryKeyAttribute.of("customers", List.of("id"))), ORDERS);
  }

  private static ScriptWriter script(Schema before, Schema after) {
    return new ScriptWriter(new SqliteDialect(), before, after).write(before.changesTo(after));
  }

  private static void run(Statement statement, ScriptWriter script) throws SQLException {
    for (String sql : script.statements()) {
      statement.execute(sql);
    }
  }

  @Test
  void rejectsNamespaces() {
    Schema after = Schema.of(new Namespace("sales", List.of(Table.of("t", "t", List.of(ID),
        PrimaryKeyAttribute.of("t", List.of("id"))))));

    assertThatThrownBy(() -> script(Schema.empty(), after)).isInstanceOf(SchemaException.class)
        .hasMessageContaining("SQLite has no namespaces");
  }

  @Test
  void writesNothingForAComment() {
    Table table = Table.of("t", "t", List.of(ID), PrimaryKeyAttribute.of("t", List.of("id")));
    Table commented = Table.of("t", "t", List.of(ID.with(new CommentAttribute("The key."))), table.primaryKey(),
        new CommentAttribute("A table."));

    assertThat(script(Schema.of(table), Schema.of(commented)).statements()).isEmpty();
  }

  @Test
  void rebuildsAnUnreferencedTableInATransaction() {
    Table before = Table.of("t", "t", List.of(ID, Column.of("2", "a", new IntegerType(IntegerType.Size.INT32))),
        PrimaryKeyAttribute.of("t", List.of("id")));
    Table after = Table.of("t", "t", List.of(ID, Column.of("2", "a", new IntegerType(IntegerType.Size.INT32),
        NotNullAttribute.INSTANCE)), PrimaryKeyAttribute.of("t", List.of("id")));

    assertThat(script(Schema.of(before), Schema.of(after)).transactional()).isTrue();
  }

  @Test
  void refusesToRebuildAReferencedTableWhileForeignKeysAreOn() throws SQLException {
    ScriptWriter script = script(customers(20), customers(40));

    assertThat(script.transactional()).isFalse();
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:?foreign_keys=true");
        Statement statement = connection.createStatement()) {
      run(statement, script(Schema.empty(), customers(20)));
      statement.execute("INSERT INTO customers VALUES (1, 'Ada')");
      statement.execute("INSERT INTO orders VALUES (1, 1)");
      connection.setAutoCommit(false);

      assertThatThrownBy(() -> run(statement, script)).isInstanceOf(SQLException.class)
          .hasMessageContaining("foreign_keys_must_be_off");

      connection.rollback();
      try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM orders")) {
        result.next();
        assertThat(result.getInt(1)).isEqualTo(1);
      }
    }
  }

  @Test
  void namesItsTemporaryObjectsWithThePrefix() {
    assertThat(String.join(" ", script(customers(20), customers(40)).statements()))
        .contains("\"leadseal_new_customers\"").contains("\"leadseal_check\"");
  }

  @Test
  void namesItsTemporaryObjectsWithoutAnEmptyPrefix() {
    Schema before = customers(20);
    Schema after = customers(40);

    ScriptWriter script = new ScriptWriter(new SqliteDialect(), before, after, "").write(before.changesTo(after));

    assertThat(String.join(" ", script.statements())).contains("\"new_customers\"").contains("\"check\"")
        .doesNotContain("leadseal");
  }
}
