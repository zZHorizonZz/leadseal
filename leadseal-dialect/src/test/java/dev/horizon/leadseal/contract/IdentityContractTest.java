package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ColumnType;
import dev.horizon.leadseal.Table;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class IdentityContractTest extends ContractTest {

  private static final Column NAME = text("2", "name", 20);

  private static Table customers(Column id) {
    return table("shop.Customer", "customers", List.of(id, NAME), "id");
  }

  private void insert(String name) throws SQLException {
    execute("INSERT INTO " + q("customers") + " (" + q("name") + ") VALUES (?)", name);
  }

  @ParameterizedTest
  @EnumSource(value = ColumnType.class, names = { "INT", "BIGINT" })
  void generatesKeys(ColumnType type) throws SQLException {
    apply(schema(customers(column("1", "id", type).withIdentity(true))));

    insert("Ada");
    insert("Grace");

    assertThat(query("SELECT " + q("id") + " FROM " + q("customers") + " WHERE " + q("name") + " = 'Grace'"))
        .containsExactly(List.of("2"));
  }

  @ParameterizedTest
  @EnumSource(value = ColumnType.class, names = { "INT", "BIGINT" })
  void continuesAfterTheExistingKeys(ColumnType type) throws SQLException {
    assumeTrue(testDatabase().supportsIdentityChanges());
    Column id = column("1", "id", type);
    apply(schema(customers(id)));
    execute("INSERT INTO " + q("customers") + " (" + q("id") + ", " + q("name") + ") VALUES (7, 'Ada')");

    apply(schema(customers(id.withIdentity(true))));
    insert("Grace");

    assertThat(query("SELECT " + q("name") + " FROM " + q("customers") + " WHERE " + q("id") + " > 7"))
        .containsExactly(List.of("Grace"));
  }
}
