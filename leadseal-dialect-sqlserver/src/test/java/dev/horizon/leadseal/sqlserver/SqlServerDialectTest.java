package dev.horizon.leadseal.sqlserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.contract.DialectExpressionTest;
import dev.horizon.leadseal.type.IntegerType;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SqlServerDialectTest {

  @Test
  void rejectsIdentityChangesOfExistingColumns() {
    Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNull.INSTANCE);
    PrimaryKey key = PrimaryKey.of("orders", List.of("id"));
    Schema before = Schema.of(Table.of("o", "orders", List.of(id), key));
    Schema after = Schema.of(Table.of("o", "orders", List.of(id.with(Identity.INSTANCE)), key));

    assertThatThrownBy(() -> new ScriptWriter(new SqlServerDialect(), before, after)
        .write(before.changesTo(after))).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("cannot add IDENTITY");
  }

  @Test
  void separatesTheBatchOfAnIsolatedStatement() {
    ScriptWriter sql = new ScriptWriter(new SqlServerDialect(), Schema.empty(), Schema.empty());
    sql.append("SELECT 1").end().append("SELECT 2").end().isolate().append("CREATE TRIGGER t").end()
        .append("SELECT 3").end().isolate().append("CREATE TRIGGER u").end();

    assertThat(sql.text()).isEqualTo("SELECT 1;\n\nSELECT 2;\nGO\n\nCREATE TRIGGER t;\nGO\n\nSELECT 3;\nGO\n\n"
        + "CREATE TRIGGER u;\n");
    assertThat(sql.statements()).containsExactly("SELECT 1", "SELECT 2", "CREATE TRIGGER t", "SELECT 3",
        "CREATE TRIGGER u");
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new SqlServerDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | (NEW.[paid] = 1)
          NOT_PAID_CONDITION             | (NOT (NEW.[paid] = 1))
          POSITIVE_CONDITION             | (NEW.[quantity] > 2)
          POSITIVE_VALUE                 | CASE WHEN (NEW.[quantity] > 2) THEN 1 ELSE 0 END
          POSITIVE_EQUALS_PAID_CONDITION | (CASE WHEN (NEW.[quantity] > 2) THEN 1 ELSE 0 END = NEW.[paid])
          TRUE_CONDITION                 | (1 = 1)
          CONCAT_VALUE                   | (NEW.[name] + N'!')
          INTEGER_DIVIDE_VALUE           | (NEW.[quantity] / 2)
          LENGTH_VALUE                   | (LEN(NEW.[name] + N'x') - 1)
          CHANGE_CONDITION               | ((OLD.[name] <> NEW.[name]) OR (OLD.[name] IS NULL AND NEW.[name] IS NOT NULL) OR (OLD.[name] IS NOT NULL AND NEW.[name] IS NULL))
          NOW_VALUE                      | SYSUTCDATETIME()
          UUID_VALUE                     | NEWID()
          """;
    }
  }
}
