package dev.horizon.leadseal.sqlserver;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.contract.CommentContractTest;
import dev.horizon.leadseal.contract.ContractTest;
import dev.horizon.leadseal.contract.DialectContract;
import dev.horizon.leadseal.contract.EnumRenameRefusalContractTest;
import dev.horizon.leadseal.contract.PartialIndexContractTest;
import dev.horizon.leadseal.contract.SchemaContractTest;
import dev.horizon.leadseal.contract.TestDatabase;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Row;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SqlServerContractTest extends DialectContract {

  SqlServerContractTest() {
    super(new SqlServerTestDatabase());
  }

  @Nested
  class Schemas extends SchemaContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Comments extends CommentContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class PartialIndexes extends PartialIndexContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Enums extends EnumRenameRefusalContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }
  }

  @Nested
  class Recursion extends ContractTest {

    @Override
    protected TestDatabase database() {
      return fixture;
    }

    @Test
    @DisplayName("Writes the rewrites once when the database runs triggers recursively")
    void rewriteOnce() throws SQLException {
      Column revision = column("2", "revision", INT).with(new LiteralDefault("0"));
      apply(schema(table("shop.Order", "orders", List.of(column("1", "id", BIGINT), revision), "id").with(Triggers.of(
          new Rewrite("revision", new Arithmetic(Arithmetic.Operator.ADD, new ColumnReference("revision", Row.OLD),
              new Literal(INT, "1")))))));
      execute("ALTER DATABASE CURRENT SET RECURSIVE_TRIGGERS ON");
      execute("INSERT INTO " + q("orders") + " (" + q("id") + ") VALUES (1)");

      execute("UPDATE " + q("orders") + " SET " + q("id") + " = " + q("id"));

      assertThat(query("SELECT " + q("revision") + " FROM " + q("orders"))).containsExactly(List.of("1"));
    }
  }
}
