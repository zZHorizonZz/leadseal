package dev.horizon.leadseal.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PostgresDialectTest {

  @ParameterizedTest(name = "({0})", quoteTextArguments = false)
  @CsvSource(delimiter = '|', quoteCharacter = '~', textBlock = """
      BEGIN RETURN NEW; END;               | $leadseal$
      SELECT '$leadseal$', '$leadseal1$'   | $leadseal2$
      """)
  void quotesABodyWithATagThatTheBodyDoesNotContain(String body, String tag) {
    assertThat(new PostgresDialect().dollarQuote(body)).isEqualTo(tag + "\n" + body + "\n" + tag);
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new PostgresDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | NEW."paid"
          NOT_PAID_CONDITION             | (NOT NEW."paid")
          POSITIVE_CONDITION             | (NEW."quantity" > 2)
          POSITIVE_VALUE                 | (NEW."quantity" > 2)
          POSITIVE_EQUALS_PAID_CONDITION | ((NEW."quantity" > 2) = NEW."paid")
          TRUE_CONDITION                 | true
          CONCAT_VALUE                   | (NEW."name" || '!')
          INTEGER_DIVIDE_VALUE           | (NEW."quantity" / 2)
          LENGTH_VALUE                   | CHAR_LENGTH(NEW."name")
          CHANGE_CONDITION               | (OLD."name" IS DISTINCT FROM NEW."name")
          NOW_VALUE                      | CURRENT_TIMESTAMP
          UUID_VALUE                     | gen_random_uuid()
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

    assertThat(new ScriptWriter(new PostgresDialect(), before, after).write(before.changesTo(after)).statements())
        .containsExactly("""
            CREATE FUNCTION "rw_posts"() RETURNS trigger LANGUAGE plpgsql AS $leadseal$
            BEGIN
              NEW."slug" := LOWER(NEW."title");
              NEW."revision" := (OLD."revision" + 1);
              NEW."label" := UPPER(NEW."slug");
              RETURN NEW;
            END;
            $leadseal$""",
            "CREATE TRIGGER \"rw_posts\" BEFORE UPDATE ON \"posts\" FOR EACH ROW EXECUTE FUNCTION \"rw_posts\"()",
            """
                CREATE FUNCTION "tg_posts_insert"() RETURNS trigger LANGUAGE plpgsql AS $leadseal$
                BEGIN
                  IF (NOT (SELECT "r"."active" FROM "authors" AS "r" WHERE "r"."id" = NEW."author_id")) THEN
                    RAISE EXCEPTION USING MESSAGE = 'The author is not active.', ERRCODE = 'P0001';
                  END IF;
                  RETURN NULL;
                END;
                $leadseal$""",
            "CREATE TRIGGER \"tg_posts_insert\" AFTER INSERT ON \"posts\" FOR EACH ROW EXECUTE FUNCTION \"tg_posts_insert\"()",
            """
                CREATE FUNCTION "tg_posts_update"() RETURNS trigger LANGUAGE plpgsql AS $leadseal$
                BEGIN
                  IF (OLD."title" IS DISTINCT FROM NEW."title") THEN
                    INSERT INTO "post_events" ("post_id", "title") VALUES (NEW."id", OLD."title");
                  END IF;
                  RETURN NULL;
                END;
                $leadseal$""",
            "CREATE TRIGGER \"tg_posts_update\" AFTER UPDATE ON \"posts\" FOR EACH ROW EXECUTE FUNCTION \"tg_posts_update\"()",
            """
                CREATE FUNCTION "tg_posts_delete"() RETURNS trigger LANGUAGE plpgsql AS $leadseal$
                BEGIN
                  IF (NOT (OLD."revision" = 0)) THEN
                    RAISE EXCEPTION USING MESSAGE = 'Revised posts stay.', ERRCODE = 'P0001';
                  END IF;
                  RETURN NULL;
                END;
                $leadseal$""",
            "CREATE TRIGGER \"tg_posts_delete\" AFTER DELETE ON \"posts\" FOR EACH ROW EXECUTE FUNCTION \"tg_posts_delete\"()");
    assertThat(new ScriptWriter(new PostgresDialect(), after, before).write(after.changesTo(before)).statements())
        .containsExactly("DROP TRIGGER \"rw_posts\" ON \"posts\"",
            "DROP FUNCTION \"rw_posts\"()",
            "DROP TRIGGER \"tg_posts_insert\" ON \"posts\"",
            "DROP FUNCTION \"tg_posts_insert\"()",
            "DROP TRIGGER \"tg_posts_update\" ON \"posts\"",
            "DROP FUNCTION \"tg_posts_update\"()",
            "DROP TRIGGER \"tg_posts_delete\" ON \"posts\"",
            "DROP FUNCTION \"tg_posts_delete\"()");
  }
}
