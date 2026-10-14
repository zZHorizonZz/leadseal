package dev.horizon.leadseal;

import static dev.horizon.leadseal.Fixtures.column;
import static dev.horizon.leadseal.Fixtures.nullable;
import static dev.horizon.leadseal.Fixtures.schema;
import static dev.horizon.leadseal.Fixtures.table;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SchemaTest {

  private static final Column ID = column("1", "id", ColumnType.BIGINT);

  static Stream<Arguments> invalid() {
    return Stream.of(
        arguments("no primary key columns", call(() -> table("t", "t").column(ID).build()), "has no columns"),
        arguments("nullable primary key", call(() -> table("t", "t").column(nullable("1", "id", ColumnType.BIGINT))
            .primaryKey("id").build()), "must not be nullable"),
        arguments("unknown primary key column", call(() -> table("t", "t").column(ID).primaryKey("other").build()),
            "does not have"),
        arguments("duplicate column name", call(() -> table("t", "t").column(ID).column(column("2", "id",
            ColumnType.INT)).primaryKey("id").build()), "column name 'id' twice"),
        arguments("length on a number", call(() -> new Column("1", "id", ColumnType.INT, 10, false)),
            "only TEXT and BYTES"),
        arguments("duplicate table name", call(() -> schema(table("a", "t").column(ID).primaryKey("id").build(),
            table("b", "t").column(ID).primaryKey("id").build())), "table name 't' twice"),
        arguments("foreign key to a missing table", call(() -> schema(table("a", "a").column(ID).primaryKey("id")
            .foreignKey("id", "missing", "id", ForeignKey.Action.CASCADE).build())), "does not have"),
        arguments("set null on a required column", call(() -> table("a", "a").column(ID).column(column("2", "b_id",
            ColumnType.BIGINT)).primaryKey("id").foreignKey("b_id", "b", "id", ForeignKey.Action.SET_NULL).build()),
            "does not accept NULL"),
        arguments("foreign key with another type", call(() -> schema(
            table("a", "a").column(ID).column(column("2", "b_id", ColumnType.INT)).primaryKey("id")
                .foreignKey("b_id", "b", "id", ForeignKey.Action.CASCADE).build(),
            table("b", "b").column(ID).primaryKey("id").build())), "The types must be the same"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalid")
  void rejects(String name, ThrowingCallable call, String message) {
    assertThatThrownBy(call).isInstanceOf(SchemaException.class).hasMessageContaining(message);
  }

  @Test
  void hashesTheSameSchemaTheSame() {
    Schema first = schema(table("a", "a").column(ID).primaryKey("id").build());
    Schema second = schema(table("a", "a").column(ID).primaryKey("id").build());

    assertThat(first.hash()).isEqualTo(second.hash()).hasSize(16);
    assertThat(first.hash()).isNotEqualTo(Schema.empty().hash());
  }

  @Test
  void shortensLongGeneratedNames() {
    String table = "a_table_with_a_very_long_name_that_goes_on";
    Index index = Index.of(table, List.of("first_column_name", "second_column_name"), false);

    assertThat(index.name()).hasSize(63).startsWith("ix_" + table);
    assertThat(Index.of(table, List.of("first_column_name", "second_column_name"), false)).isEqualTo(index);
  }

  private static ThrowingCallable call(ThrowingCallable call) {
    return call;
  }
}
