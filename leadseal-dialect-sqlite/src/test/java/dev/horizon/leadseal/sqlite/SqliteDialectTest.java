package dev.horizon.leadseal.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Diff;
import dev.horizon.leadseal.ForeignKey;
import dev.horizon.leadseal.PrimaryKey;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SqlScript;
import dev.horizon.leadseal.Table;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqliteDialectTest {

  private static final Column ID = new Column("1", "id", ColumnType.BIGINT, null, false);
  private static final Table ORDERS = new Table("shop.Order", "orders", List.of(ID, new Column("2", "customer_id",
      ColumnType.BIGINT, null, false)), PrimaryKey.of("orders", List.of("id")), List.of(), List.of(
          ForeignKey.of(
              "orders", List.of("customer_id"), "customers", List.of("id"), ForeignKey.Action.CASCADE)));

  private static Schema customers(int length) {
    return new Schema(List.of(new Table("shop.Customer", "customers", List.of(ID, new Column("2", "name",
        ColumnType.TEXT, length, false)), PrimaryKey.of("customers", List.of("id")), List.of(), List.of()), ORDERS));
  }

  private static SqlScript script(Schema before, Schema after) {
    return DdlWriter.script(new SqliteDialect(), new Diff(before, after).operations(), before, after);
  }

  private static void run(Statement statement, SqlScript script) throws SQLException {
    for (String sql : script.statements()) {
      statement.execute(sql);
    }
  }

  @Test
  void rebuildsAnUnreferencedTableInATransaction() {
    Schema before = new Schema(List.of(new Table("t", "t", List.of(ID, new Column("2", "a", ColumnType.INT, null,
        true)), PrimaryKey.of("t", List.of("id")), List.of(), List.of())));
    Schema after = new Schema(List.of(new Table("t", "t", List.of(ID, new Column("2", "a", ColumnType.INT, null,
        false)), PrimaryKey.of("t", List.of("id")), List.of(), List.of())));

    assertThat(script(before, after).transactional()).isTrue();
  }

  @Test
  void refusesToRebuildAReferencedTableWhileForeignKeysAreOn() throws SQLException {
    SqlScript script = script(customers(20), customers(40));

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
}
