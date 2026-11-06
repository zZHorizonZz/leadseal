package dev.horizon.leadseal.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.contract.DialectExpressionTest;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.ForeignColumn;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SqliteDialectTest {

  private static final Column ID = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64),
      NotNull.INSTANCE);
  private static final Table ORDERS = Table.of("shop.Order", "orders", List.of(ID, Column.of("2", "customer_id",
      ID.type(), NotNull.INSTANCE)), PrimaryKey.of("orders", List.of("id")),
      ForeignKey.of("orders", List.of("customer_id"), TableName.of("customers"), List.of("id"),
          ForeignKey.Action.CASCADE));

  private static Schema customers(int length) {
    return Schema.of(Table.of("shop.Customer", "customers", List.of(ID, Column.of("2", "name", TextType.of(length),
        NotNull.INSTANCE)), PrimaryKey.of("customers", List.of("id"))), ORDERS);
  }

  private static ScriptWriter script(Schema before, Schema after) {
    return new ScriptWriter(new SqliteDialect(), before, after).write(before.changesTo(after));
  }

  private void run(Statement statement, ScriptWriter script) throws SQLException {
    for (String sql : script.statements()) {
      statement.execute(sql);
    }
  }

  @Test
  @DisplayName("Rejects namespaces")
  void rejectNamespaces() {
    Schema after = Schema.of(new Namespace("sales", List.of(Table.of("t", "t", List.of(ID),
        PrimaryKey.of("t", List.of("id"))))));

    assertThatThrownBy(() -> script(Schema.empty(), after)).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("SQLite has no namespaces");
  }

  @Test
  @DisplayName("Writes nothing for a comment")
  void skipComments() {
    Table table = Table.of("t", "t", List.of(ID), PrimaryKey.of("t", List.of("id")));
    Table commented = Table.of("t", "t", List.of(ID.with(new Comment("The key."))), table.primaryKey(),
        new Comment("A table."));

    assertThat(script(Schema.of(table), Schema.of(commented)).statements()).isEmpty();
  }

  @Test
  @DisplayName("Rebuilds an unreferenced table in a transaction")
  void rebuildInTransaction() {
    Table before = Table.of("t", "t", List.of(ID, Column.of("2", "a", new IntegerType(IntegerType.Size.INT32))),
        PrimaryKey.of("t", List.of("id")));
    Table after = Table.of("t", "t", List.of(ID, Column.of("2", "a", new IntegerType(IntegerType.Size.INT32),
        NotNull.INSTANCE)), PrimaryKey.of("t", List.of("id")));

    assertThat(script(Schema.of(before), Schema.of(after)).transactional()).isTrue();
  }

  @Test
  @DisplayName("Refuses to rebuild a referenced table while foreign keys are on")
  void refuseReferencedRebuild() throws SQLException {
    ScriptWriter script = script(customers(20), customers(40));

    assertThat(script.transactional()).isFalse();
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:?foreign_keys=true");
        Statement statement = connection.createStatement()) {
      run(statement, script(Schema.empty(), customers(20)));
      statement.execute("INSERT INTO customers VALUES (1, 'Ada')");
      statement.execute("INSERT INTO orders VALUES (1, 1)");
      connection.setAutoCommit(false);

      assertThatThrownBy(() -> run(statement, script)).isInstanceOf(SQLException.class)
          .hasMessageContaining("foreign_keys_must_be_off");

      connection.rollback();
      try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM orders")) {
        result.next();
        assertThat(result.getInt(1)).isEqualTo(1);
      }
    }
  }

  @Test
  @DisplayName("Names its temporary objects with the prefix")
  void prefixTemporaryObjects() {
    assertThat(String.join(" ", script(customers(20), customers(40)).statements()))
        .contains("\"leadseal_new_customers\"").contains("\"leadseal_check\"");
  }

  @Test
  @DisplayName("Names its temporary objects without an empty prefix")
  void omitEmptyPrefix() {
    Schema before = customers(20);
    Schema after = customers(40);

    ScriptWriter script = new ScriptWriter(new SqliteDialect(), before, after, "").write(before.changesTo(after));

    assertThat(String.join(" ", script.statements())).contains("\"new_customers\"").contains("\"check\"")
        .doesNotContain("leadseal");
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new SqliteDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | NEW."paid"
          NOT_PAID_CONDITION             | (NOT NEW."paid")
          POSITIVE_CONDITION             | (NEW."quantity" > 2)
          POSITIVE_VALUE                 | (NEW."quantity" > 2)
          POSITIVE_EQUALS_PAID_CONDITION | ((NEW."quantity" > 2) = NEW."paid")
          TRUE_CONDITION                 | 1
          CONCAT_VALUE                   | (NEW."name" || '!')
          INTEGER_DIVIDE_VALUE           | (NEW."quantity" / 2)
          LENGTH_VALUE                   | length(NEW."name")
          CHANGE_CONDITION               | (OLD."name" IS NOT NEW."name")
          NOW_VALUE                      | (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
          UUID_VALUE                     | (lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (abs(random()) % 4), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))))
          """;
    }
  }

  @Test
  @DisplayName("Writes the rewrites and the triggers of a table as database triggers")
  void writeTriggers() {
    IntegerType bigint = new IntegerType(IntegerType.Size.INT64);
    Column id = Column.of("1", "id", bigint, NotNull.INSTANCE);
    Table authors = Table.of("blog.Author", "authors", List.of(id, Column.of("2", "active", new BooleanType(),
        NotNull.INSTANCE)), PrimaryKey.of("authors", List.of("id")));
    Table events = Table.of("blog.PostEvent", "post_events", List.of(id.with(Identity.INSTANCE), Column.of("2",
        "post_id", bigint, NotNull.INSTANCE), Column.of("3", "title", TextType.of(100))), PrimaryKey.of("post_events",
            List.of("id")));
    Table posts = Table.of("blog.Post", "posts", List.of(id, Column.of("2", "title", TextType.of(100),
        NotNull.INSTANCE), Column.of("3", "slug", TextType.of(100)), Column.of("4", "label", TextType.of(100)),
        Column.of("5", "revision", bigint, NotNull.INSTANCE, new LiteralDefault("0")), Column.of("6", "author_id",
            bigint)),
        PrimaryKey.of("posts", List.of("id")), ForeignKey.of("posts", List.of("author_id"), TableName.of(
            "authors"), List.of("id"), ForeignKey.Action.NO_ACTION));
    Triggers triggers = new Triggers(List.of(new Rewrite("label", new Call(Call.Function.UPPER, List.of(ColumnReference
        .of("slug")))), new Rewrite("slug", new Call(Call.Function.LOWER, List.of(ColumnReference.of("title")))),
        new Rewrite("revision", new Arithmetic(Arithmetic.Operator.ADD, new ColumnReference("revision", Row.OLD),
            new Literal(bigint, "1")))),
        List.of(new Trigger("log", List.of(Mutation.UPDATE), new ColumnChange("title"),
            List.of(new Trigger.Insert(TableName.of("post_events"), List.of(new Trigger.Assignment("post_id", ColumnReference
                .of("id")), new Trigger.Assignment("title", new ColumnReference("title", Row.OLD)))))),
            new Trigger("active", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Assert(new ForeignColumn(
                ColumnReference.of("author_id"), TableName.of("authors"), "id", "active"), "The author is not active."))),
            new Trigger("keep", List.of(Mutation.DELETE), Trigger.ALWAYS, List.of(new Trigger.Assert(new Comparison(
                Comparison.Operator.EQUALS, ColumnReference.of("revision"), new Literal(bigint, "0")),
                "Revised posts stay.")))));
    Schema before = Schema.of(authors, events, posts);
    Schema after = Schema.of(authors, events, posts.with(triggers));

    assertThat(new ScriptWriter(new SqliteDialect(), before, after).write(before.changesTo(after)).statements())
        .containsExactly(
            """
                CREATE TRIGGER "tg_posts_insert" AFTER INSERT ON "posts" BEGIN
                  SELECT RAISE(ABORT, 'The author is not active.') FROM "posts" WHERE "posts"."id" = NEW."id" AND (NOT (SELECT "r"."active" FROM "authors" AS "r" WHERE "r"."id" = "posts"."author_id"));
                END""",
            """
                CREATE TRIGGER "tg_posts_update" AFTER UPDATE ON "posts" BEGIN
                  UPDATE "posts" SET "slug" = LOWER("posts"."title"), "revision" = (OLD."revision" + 1) WHERE "posts"."id" = NEW."id";
                  UPDATE "posts" SET "label" = UPPER("posts"."slug") WHERE "posts"."id" = NEW."id";
                  INSERT INTO "post_events" ("post_id", "title") SELECT "posts"."id", OLD."title" FROM "posts" WHERE "posts"."id" = NEW."id" AND (OLD."title" IS NOT "posts"."title");
                END""",
            """
                CREATE TRIGGER "tg_posts_delete" AFTER DELETE ON "posts" BEGIN
                  SELECT RAISE(ABORT, 'Revised posts stay.') WHERE (NOT (OLD."revision" = 0));
                END""");
    assertThat(new ScriptWriter(new SqliteDialect(), after, before).write(after.changesTo(before)).statements())
        .containsExactly("DROP TRIGGER \"tg_posts_insert\"",
            "DROP TRIGGER \"tg_posts_update\"",
            "DROP TRIGGER \"tg_posts_delete\"");
  }
}
