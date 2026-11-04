package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.type.TextType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class ScriptContractTest extends ContractTest {

  static final TableName ITEMS = TableName.of("items");
  static final TableName LOG = TableName.of("log");

  static Schema schema() {
    Table items = table("shop.Item", "items", List.of(column("1", "id", BIGINT)), "id");
    Table log = table("shop.Log", "log", List.of(column("1", "item", BIGINT), column("2", "note",
        TextType.of(20))), "item", "note");
    return Schema.of(items, log);
  }

  static ScriptWriter script(TestDatabase database) {
    Dialect dialect = database.dialect();
    Schema schema = schema();
    ScriptWriter sql = new ScriptWriter(dialect, schema, schema);
    insert(sql, 1).render(database.trigger(ITEMS, LOG)).end();
    return insert(sql, 2);
  }

  private static ScriptWriter insert(ScriptWriter sql, int id) {
    return sql.append("INSERT INTO ").table(ITEMS).append(" (").name("id").append(") VALUES (" + id + ")").end();
  }

  @Test
  void runsAStatementWithABodyBetweenOtherStatements() throws IOException, SQLException {
    apply(schema());
    Path file = migrations.resolve("99990101000000").resolve(dialect.name()).resolve("V99990101000000__trigger.sql");
    Files.createDirectories(file.getParent());
    Files.writeString(file, script(testDatabase()).text());

    assertThat(migrate()).isEqualTo(1);

    assertThat(query("SELECT " + q("id") + " FROM " + q("items") + " ORDER BY " + q("id")))
        .containsExactly(List.of("1"), List.of("2"));
    assertThat(query("SELECT " + q("item") + ", " + q("note") + " FROM " + q("log") + " ORDER BY " + q("note")))
        .containsExactly(List.of("2", "first"), List.of("2", "second"));
  }
}
