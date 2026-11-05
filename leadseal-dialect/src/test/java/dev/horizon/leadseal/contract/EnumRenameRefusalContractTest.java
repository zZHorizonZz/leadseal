package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.type.EnumType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The renames of enum values, for a dialect whose enum columns store the value names. */
public abstract class EnumRenameRefusalContractTest extends EnumContractTest {

  @Test
  @DisplayName("Refuses to rename a value of an enum that stores names")
  void refuseRename() {
    apply(schema(shipments(state(names(NEW, SENT)))));

    assertThatThrownBy(() -> generate(schema(shipments(state(names(NEW, new EnumType.Value(1, "SHIPPED")))))))
        .isInstanceOf(UnsupportedChangeException.class).hasMessageContaining("renames the value SENT to SHIPPED");
  }
}
