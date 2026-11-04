package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.type.Type;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class IdentityContractTest extends ContractTest {

  private static final Column NAME = text("2", "name", 20);

  private static Table customers(Column id) {
    return table("shop.Customer", "customers", List.of(id, NAME), "id");
  }

  private void insert(String name) throws SQLException {
    execute("INSERT INTO " + q("customers") + " (" + q("name") + ") VALUES (?)", name);
  }

  @ParameterizedTest
  @MethodSource("integers")
  void generatesKeys(Type type) throws SQLException {
    apply(schema(customers(column("1", "id", type).with(Identity.INSTANCE))));

    insert("Ada");
    insert("Grace");

    assertThat(query("SELECT " + q("id") + " FROM " + q("customers") + " WHERE " + q("name") + " = 'Grace'"))
        .containsExactly(List.of("2"));
  }
}
