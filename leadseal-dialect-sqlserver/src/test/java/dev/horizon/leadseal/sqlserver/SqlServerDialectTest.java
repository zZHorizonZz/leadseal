package dev.horizon.leadseal.sqlserver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Diff;
import dev.horizon.leadseal.PrimaryKey;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqlServerDialectTest {

  @Test
  void rejectsIdentityChangesOfExistingColumns() {
    Column id = new Column("1", "id", ColumnType.BIGINT, null, false);
    Schema before = new Schema(List.of(new Table("o", "orders", List.of(id), PrimaryKey.of("orders", List.of("id")),
        List.of(), List.of())));
    Schema after = new Schema(List.of(new Table("o", "orders", List.of(id.withIdentity(true)), PrimaryKey.of(
        "orders", List.of("id")), List.of(), List.of())));

    assertThatThrownBy(() -> DdlWriter.script(new SqlServerDialect(), new Diff(before, after).operations(), before,
        after)).isInstanceOf(SchemaException.class).hasMessageContaining("cannot add IDENTITY");
  }
}
