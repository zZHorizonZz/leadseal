package dev.horizon.leadseal.mariadb;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.DdlWriter;
import dev.horizon.leadseal.Diff;
import dev.horizon.leadseal.Index;
import dev.horizon.leadseal.PrimaryKey;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.List;
import org.junit.jupiter.api.Test;

class MariaDbDialectTest {

  @Test
  void rejectsPartialIndexes() {
    Column id = new Column("1", "id", ColumnType.BIGINT, null, false);
    Table orders = new Table("o", "orders", List.of(id), PrimaryKey.of("orders", List.of("id")),
        List.of(Index.of("orders", List.of("id"), false).withWhere("id > 0")), List.of());
    Schema after = new Schema(List.of(orders));

    assertThatThrownBy(() -> DdlWriter.script(new MariaDbDialect(), new Diff(Schema.empty(), after).operations(),
        Schema.empty(), after)).isInstanceOf(SchemaException.class).hasMessageContaining("no partial indexes");
  }
}
