package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.type.EnumType;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class EnumRenameContractTest extends EnumContractTest {

  @Test
  @DisplayName("Renames a value of an enum that stores names, in the rows too")
  void renameName() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT)))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, new EnumType.Value(1, "SHIPPED"))))));

    assertThat(states("shipments")).containsExactly(List.of("SHIPPED"));
  }

  @Test
  @DisplayName("Renames a value and removes another in one migration")
  void renameAndRemove() throws SQLException {
    apply(schema(shipments(state(names(NEW, SENT, LOST)))));
    insert(1, "SENT");

    apply(schema(shipments(state(names(NEW, new EnumType.Value(1, "SHIPPED"))))));

    assertThat(states("shipments")).containsExactly(List.of("SHIPPED"));
  }
}
