package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.type.Type;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class IdentityChangeContractTest extends ContractTest {

  private static final Column NAME = text("2", "name", 20);

  private static Table customers(Column id) {
    return table("shop.Customer", "customers", List.of(id, NAME), "id");
  }

  private void insert(String name) throws SQLException {
    execute("INSERT INTO " + q("customers") + " (" + q("name") + ") VALUES (?)", name);
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("integers")
  @DisplayName("Continues after the existing keys")
  void continueAfterKeys(Type type) throws SQLException {
    Column id = column("1", "id", type);
    apply(schema(customers(id)));
    execute("INSERT INTO " + q("customers") + " (" + q("id") + ", " + q("name") + ") VALUES (7, 'Ada')");

    apply(schema(customers(id.with(Identity.INSTANCE))));
    insert("Grace");

    assertThat(query("SELECT " + q("name") + " FROM " + q("customers") + " WHERE " + q("id") + " > 7"))
        .containsExactly(List.of("Grace"));
  }
}
