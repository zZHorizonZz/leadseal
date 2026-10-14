package dev.horizon.leadseal.mariadb;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import java.util.List;
import org.junit.jupiter.api.Test;

class MariaDbDialectTest {

  @Test
  void rejectsPartialIndexes() {
    Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNullAttribute.INSTANCE);
    Table orders = Table.of("o", "orders", List.of(id), PrimaryKeyAttribute.of("orders", List.of("id")),
        IndexAttribute.of("orders", List.of("id"), false).withWhere("id > 0"));
    Schema after = Schema.of(orders);

    assertThatThrownBy(() -> new ScriptWriter(new MariaDbDialect(), Schema.empty(), after)
        .write(Schema.empty().changesTo(after))).isInstanceOf(SchemaException.class)
        .hasMessageContaining("no partial indexes");
  }
}
