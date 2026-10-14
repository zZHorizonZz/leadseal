package dev.horizon.leadseal.sqlserver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.IntegerType;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqlServerDialectTest {

  @Test
  void rejectsIdentityChangesOfExistingColumns() {
    Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNullAttribute.INSTANCE);
    PrimaryKeyAttribute key = PrimaryKeyAttribute.of("orders", List.of("id"));
    Schema before = Schema.of(Table.of("o", "orders", List.of(id), key));
    Schema after = Schema.of(Table.of("o", "orders", List.of(id.with(IdentityAttribute.INSTANCE)), key));

    assertThatThrownBy(() -> new ScriptWriter(new SqlServerDialect(), before, after)
        .write(before.changesTo(after))).isInstanceOf(SchemaException.class)
        .hasMessageContaining("cannot add IDENTITY");
  }
}
