package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.LiteralDefaultAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.attribute.RandomUuidDefaultAttribute;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SchemaTest {

  private static final Type BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final Type INT = new IntegerType(IntegerType.Size.INT32);
  private static final Column ID = Column.of("1", "id", BIGINT, NotNullAttribute.INSTANCE);
  private static final PrimaryKeyAttribute KEY = PrimaryKeyAttribute.of("t", List.of("id"));

  static Stream<Arguments> invalid() {
    return Stream.of(
        arguments("no primary key", call(() -> Table.of("t", "t", List.of(ID))), "has no primary key"),
        arguments("nullable primary key", call(() -> Table.of("t", "t", List.of(Column.of("1", "id", BIGINT)), KEY)),
            "must not accept NULL"),
        arguments("unknown primary key column", call(() -> Table.of("t", "t", List.of(ID),
            PrimaryKeyAttribute.of("t", List.of("other")))), "does not have"),
        arguments("duplicate column name", call(() -> Table.of("t", "t", List.of(ID, Column.of("2", "id", INT)),
            KEY)), "column name id twice"),
        arguments("duplicate attribute", call(() -> Column.of("1", "id", BIGINT, NotNullAttribute.INSTANCE,
            NotNullAttribute.INSTANCE)), "two attributes with the key"),
        arguments("duplicate table name", call(() -> Schema.of(Table.of("a", "t", List.of(ID), KEY),
            Table.of("b", "t", List.of(ID), KEY))), "has the table 't' twice"),
        arguments("duplicate table id", call(() -> Schema.of(Namespace.defaults(Table.of("a", "t", List.of(ID), KEY)),
            new Namespace("sales", List.of(Table.of("a", "u", List.of(ID), KEY))))), "table id 'a' twice"),
        arguments("foreign key to a missing table", call(() -> Schema.of(Table.of("a", "a", List.of(ID),
            PrimaryKeyAttribute.of("a", List.of("id")), ForeignKeyAttribute.of("a", List.of("id"),
                TableName.of("missing"), List.of("id"), ForeignKeyAttribute.Action.CASCADE)))),
            "does not have"),
        arguments("set null on a required column", call(() -> Table.of("a", "a", List.of(ID, Column.of("2", "b_id",
            BIGINT, NotNullAttribute.INSTANCE)), PrimaryKeyAttribute.of("a", List.of("id")),
            ForeignKeyAttribute.of("a", List.of("b_id"), TableName.of("b"), List.of("id"),
                ForeignKeyAttribute.Action.SET_NULL))),
            "does not accept NULL"),
        arguments("foreign key with another type", call(() -> Schema.of(
            Table.of("a", "a", List.of(ID, Column.of("2", "b_id", INT, NotNullAttribute.INSTANCE)),
                PrimaryKeyAttribute.of("a", List.of("id")), ForeignKeyAttribute.of("a", List.of("b_id"),
                    TableName.of("b"), List.of("id"), ForeignKeyAttribute.Action.CASCADE)),
            Table.of("b", "b", List.of(ID), PrimaryKeyAttribute.of("b", List.of("id"))))),
            "The types must be the same"),
        arguments("identity outside the primary key", call(() -> Table.of("t", "t", List.of(ID, Column.of("2", "n",
            INT, NotNullAttribute.INSTANCE, IdentityAttribute.INSTANCE)), KEY)),
            "must be the only column of the primary key"),
        arguments("identity on text", call(() -> Column.of("1", "id", new TextType(null), NotNullAttribute.INSTANCE,
            IdentityAttribute.INSTANCE)), "must be an integer column"),
        arguments("scale above precision", call(() -> new DecimalType(4, 5)), "between 0 and its precision"),
        arguments("default of another type", call(() -> Column.of("1", "n", INT, new LiteralDefaultAttribute(
            "many"))), "is not a value of its type"),
        arguments("integer default out of range", call(() -> Column.of("1", "n", INT, new LiteralDefaultAttribute(
            "3000000000"))), "is not a value of its type"),
        arguments("current time on a number", call(() -> Column.of("1", "n", INT, new NowDefaultAttribute())),
            "only timestamp and date columns"),
        arguments("random uuid on text", call(() -> Column.of("1", "n", new TextType(null),
            new RandomUuidDefaultAttribute())), "only UUID columns"),
        arguments("invalid json default", call(() -> Column.of("1", "n", new JsonType(), new LiteralDefaultAttribute(
            "{"))), "is not a value of its type"),
        arguments("collation on a number", call(() -> Column.of("1", "n", INT, new CollationAttribute("postgresql",
            "C"))), "only text columns"),
        arguments("descending column outside the index", call(() -> IndexAttribute.of("t", List.of("a"), false)
            .withDescending(List.of("b"))), "does not index it"),
        arguments("duplicate check name", call(() -> Table.of("t", "t", List.of(ID), KEY,
            new CheckAttribute("ck", "id > 0"), new CheckAttribute("ck", "id < 9"))), "twice"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalid")
  void rejects(String name, ThrowingCallable call, String message) {
    assertThatThrownBy(call).isInstanceOf(SchemaException.class).hasMessageContaining(message);
  }

  @Test
  void allowsTheSameTableNameInTwoNamespaces() {
    Schema schema = Schema.of(Namespace.defaults(Table.of("a", "t", List.of(ID), KEY)),
        new Namespace("sales", List.of(Table.of("b", "t", List.of(ID), KEY))));

    assertThat(schema.names()).containsExactly(TableName.of("t"), new TableName("sales", "t"));
    assertThat(schema.nameOf("b")).contains(new TableName("sales", "t"));
  }

  @Test
  void shortensLongGeneratedNames() {
    String table = "a_table_with_a_very_long_name_that_goes_on";
    IndexAttribute index = IndexAttribute.of(table, List.of("first_column_name", "second_column_name"), false);

    assertThat(index.name()).hasSize(63).startsWith("ix_" + table);
    assertThat(IndexAttribute.of(table, List.of("first_column_name", "second_column_name"), false)).isEqualTo(index);
  }

  private static ThrowingCallable call(ThrowingCallable call) {
    return call;
  }
}
