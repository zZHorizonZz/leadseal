package dev.horizon.leadseal.migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.function.Function;
import java.util.function.Supplier;

sealed interface SchemaHash {

  SchemaHash INITIAL = new Initial();

  @JsonCreator
  static SchemaHash of(String text) {
    return text == null ? INITIAL : new Value(text);
  }

  <T> T map(Function<String, T> value, Supplier<T> initial);

  @JsonValue
  default String json() {
    return map(text -> text, () -> null);
  }

  record Initial() implements SchemaHash {

    @Override
    public <T> T map(Function<String, T> value, Supplier<T> initial) {
      return initial.get();
    }

    @Override
    public String toString() {
      return "before the first migration";
    }
  }

  record Value(String text) implements SchemaHash {

    @Override
    public <T> T map(Function<String, T> value, Supplier<T> initial) {
      return value.apply(text);
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
