package dev.horizon.leadseal.mariadb;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.contract.DialectExpressionTest;
import dev.horizon.leadseal.type.IntegerType;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MariaDbDialectTest {

  @Test
  void rejectsPartialIndexes() {
    Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNull.INSTANCE);
    Table orders = Table.of("o", "orders", List.of(id), PrimaryKey.of("orders", List.of("id")),
        Index.of("orders", List.of("id"), false).withWhere("id > 0"));
    Schema after = Schema.of(orders);

    assertThatThrownBy(() -> new ScriptWriter(new MariaDbDialect(), Schema.empty(), after)
        .write(Schema.empty().changesTo(after))).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("no partial indexes");
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new MariaDbDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | NEW.`paid`
          NOT_PAID_CONDITION             | (NOT NEW.`paid`)
          POSITIVE_CONDITION             | (NEW.`quantity` > 2)
          POSITIVE_VALUE                 | (NEW.`quantity` > 2)
          POSITIVE_EQUALS_PAID_CONDITION | ((NEW.`quantity` > 2) = NEW.`paid`)
          TRUE_CONDITION                 | TRUE
          CONCAT_VALUE                   | CONCAT(NEW.`name`, '!')
          INTEGER_DIVIDE_VALUE           | (NEW.`quantity` DIV 2)
          LENGTH_VALUE                   | CHAR_LENGTH(NEW.`name`)
          CHANGE_CONDITION               | (NOT (OLD.`name` <=> NEW.`name`))
          NOW_VALUE                      | (UTC_TIMESTAMP(6))
          UUID_VALUE                     | (UUID())
          """;
    }
  }
}
