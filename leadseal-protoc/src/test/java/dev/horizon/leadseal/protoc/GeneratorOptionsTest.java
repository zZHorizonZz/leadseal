package dev.horizon.leadseal.protoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.ConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneratorOptionsTest {

  @Test
  @DisplayName("Refuses unsafe changes by default")
  void validate() {
    assertThat(GeneratorOptions.parse("migrations=db,dialect=postgresql").unsafe()).isFalse();
  }

  @Test
  @DisplayName("Applies unsafe changes with allow_unsafe")
  void allowUnsafe() {
    assertThat(GeneratorOptions.parse("migrations=db,dialect=postgresql,allow_unsafe=true").unsafe()).isTrue();
  }

  @Test
  @DisplayName("Refuses an allow_unsafe that is neither true nor false")
  void validateValue() {
    assertThatThrownBy(() -> GeneratorOptions.parse("migrations=db,dialect=postgresql,allow_unsafe=yes"))
        .isInstanceOf(ConfigurationException.class).hasMessageContaining("allow_unsafe is true or false");
  }

  @Test
  @DisplayName("Reads --allow-unsafe from the command line")
  void readUnsafeArgument() {
    assertThat(GeneratorOptions.of("", "--migrations", "db", "--dialect", "postgresql", "--allow-unsafe").unsafe())
        .isTrue();
  }
}
