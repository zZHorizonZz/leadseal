package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.NowDefault;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.RandomUuidDefault;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.Type;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SchemaTest {

  private static final Type BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final Type INT = new IntegerType(IntegerType.Size.INT32);
  private static final Column ID = Column.of("1", "id", BIGINT, NotNull.INSTANCE);
  private static final PrimaryKey KEY = PrimaryKey.of("t", List.of("id"));

  static Stream<Arguments> invalid() {
    return Stream.of(
        arguments("no primary key", call(() -> Table.of("t", "t", List.of(ID))), "has no primary key"),
        arguments("nullable primary key", call(() -> Table.of("t", "t", List.of(Column.of("1", "id", BIGINT)), KEY)),
            "must not accept NULL"),
        arguments("unknown primary key column", call(() -> Table.of("t", "t", List.of(ID),
            PrimaryKey.of("t", List.of("other")))), "does not have"),
        arguments("duplicate column name", call(() -> Table.of("t", "t", List.of(ID, Column.of("2", "id", INT)),
            KEY)), "column name id twice"),
        arguments("duplicate attribute", call(() -> Column.of("1", "id", BIGINT, NotNull.INSTANCE,
            NotNull.INSTANCE)), "two attributes with the key"),
        arguments("duplicate table name", call(() -> Schema.of(Table.of("a", "t", List.of(ID), KEY),
            Table.of("b", "t", List.of(ID), KEY))), "has the table 't' twice"),
        arguments("duplicate table id", call(() -> Schema.of(Namespace.defaults(Table.of("a", "t", List.of(ID), KEY)),
            new Namespace("sales", List.of(Table.of("a", "u", List.of(ID), KEY))))), "table id 'a' twice"),
        arguments("foreign key to a missing table", call(() -> Schema.of(Table.of("a", "a", List.of(ID),
            PrimaryKey.of("a", List.of("id")), ForeignKey.of("a", List.of("id"),
                TableName.of("missing"), List.of("id"), ForeignKey.Action.CASCADE)))),
            "does not have"),
        arguments("set null on a required column", call(() -> Table.of("a", "a", List.of(ID, Column.of("2", "b_id",
            BIGINT, NotNull.INSTANCE)), PrimaryKey.of("a", List.of("id")),
            ForeignKey.of("a", List.of("b_id"), TableName.of("b"), List.of("id"),
                ForeignKey.Action.SET_NULL))),
            "does not accept NULL"),
        arguments("foreign key with another type", call(() -> Schema.of(
            Table.of("a", "a", List.of(ID, Column.of("2", "b_id", INT, NotNull.INSTANCE)),
                PrimaryKey.of("a", List.of("id")), ForeignKey.of("a", List.of("b_id"),
                    TableName.of("b"), List.of("id"), ForeignKey.Action.CASCADE)),
            Table.of("b", "b", List.of(ID), PrimaryKey.of("b", List.of("id"))))),
            "The types must be the same"),
        arguments("identity outside the primary key", call(() -> Table.of("t", "t", List.of(ID, Column.of("2", "n",
            INT, NotNull.INSTANCE, Identity.INSTANCE)), KEY)),
            "must be the only column of the primary key"),
        arguments("identity on text", call(() -> Column.of("1", "id", TextType.unlimited(), NotNull.INSTANCE,
            Identity.INSTANCE)), "must be an integer column"),
        arguments("scale above precision", call(() -> new DecimalType(4, 5)), "between 0 and its precision"),
        arguments("default of another type", call(() -> Column.of("1", "n", INT, new LiteralDefault(
            "many"))), "is not a value of its type"),
        arguments("integer default out of range", call(() -> Column.of("1", "n", INT, new LiteralDefault(
            "3000000000"))), "is not a value of its type"),
        arguments("current time on a number", call(() -> Column.of("1", "n", INT, new NowDefault())),
            "only timestamp and date columns"),
        arguments("random uuid on text", call(() -> Column.of("1", "n", TextType.unlimited(),
            new RandomUuidDefault())), "only UUID columns"),
        arguments("invalid json default", call(() -> Column.of("1", "n", new JsonType(), new LiteralDefault(
            "{"))), "is not a value of its type"),
        arguments("collation on a number", call(() -> Column.of("1", "n", INT, new Collation("postgresql",
            "C"))), "only text columns"),
        arguments("descending column outside the index", call(() -> Index.of("t", List.of("a"), false)
            .withDescending(List.of("b"))), "does not index it"),
        arguments("duplicate check name", call(() -> Table.of("t", "t", List.of(ID), KEY,
            new Check("ck", "id > 0"), new Check("ck", "id < 9"))), "twice"));
  }

  @ParameterizedTest(name = "({0})", quoteTextArguments = false)
  @MethodSource("invalid")
  @DisplayName("Refuses a schema that breaks a rule")
  void reject(String name, ThrowingCallable call, String message) {
    assertThatThrownBy(call).isInstanceOf(InvalidSchemaException.class).hasMessageContaining(message);
  }

  @Test
  @DisplayName("Allows the same table name in two namespaces")
  void allowSameName() {
    Schema schema = Schema.of(Namespace.defaults(Table.of("a", "t", List.of(ID), KEY)),
        new Namespace("sales", List.of(Table.of("b", "t", List.of(ID), KEY))));

    assertThat(schema.names()).containsExactly(TableName.of("t"), new TableName("sales", "t"));
    assertThat(schema.nameOf("b")).contains(new TableName("sales", "t"));
  }

  @Test
  @DisplayName("Shortens long generated names")
  void shortenNames() {
    String table = "a_table_with_a_very_long_name_that_goes_on";
    Index index = Index.of(table, List.of("first_column_name", "second_column_name"), false);

    assertThat(index.name()).hasSize(63).startsWith("ix_" + table);
    assertThat(Index.of(table, List.of("first_column_name", "second_column_name"), false)).isEqualTo(index);
  }

  private static ThrowingCallable call(ThrowingCallable call) {
    return call;
  }
}
