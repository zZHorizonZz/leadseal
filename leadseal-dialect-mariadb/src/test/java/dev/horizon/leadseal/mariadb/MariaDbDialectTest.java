package dev.horizon.leadseal.mariadb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.UnsupportedChangeException;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.Index;
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

class MariaDbDialectTest {

  @Test
  @DisplayName("Rejects partial indexes")
  void rejectPartialIndexes() {
    Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNull.INSTANCE);
    Table orders = Table.of("o", "orders", List.of(id), PrimaryKey.of("orders", List.of("id")),
        Index.of("orders", List.of("id"), false).withWhere("id > 0"));
    Schema after = Schema.of(orders);

    assertThatThrownBy(() -> new ScriptWriter(new MariaDbDialect(), Schema.empty(), after)
        .write(Schema.empty().changesTo(after))).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("no partial indexes");
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new MariaDbDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | NEW.`paid`
          NOT_PAID_CONDITION             | (NOT NEW.`paid`)
          POSITIVE_CONDITION             | (NEW.`quantity` > 2)
          POSITIVE_VALUE                 | (NEW.`quantity` > 2)
          POSITIVE_EQUALS_PAID_CONDITION | ((NEW.`quantity` > 2) = NEW.`paid`)
          TRUE_CONDITION                 | TRUE
          CONCAT_VALUE                   | CONCAT(NEW.`name`, '!')
          INTEGER_DIVIDE_VALUE           | (NEW.`quantity` DIV 2)
          LENGTH_VALUE                   | CHAR_LENGTH(NEW.`name`)
          CHANGE_CONDITION               | (NOT (OLD.`name` <=> NEW.`name`))
          NOW_VALUE                      | (UTC_TIMESTAMP(6))
          UUID_VALUE                     | (UUID())
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

    assertThat(new ScriptWriter(new MariaDbDialect(), before, after).write(before.changesTo(after)).statements())
        .containsExactly("""
            CREATE TRIGGER `rw_posts` BEFORE UPDATE ON `posts` FOR EACH ROW BEGIN
              SET NEW.`slug` = LOWER(NEW.`title`);
              SET NEW.`revision` = (OLD.`revision` + 1);
              SET NEW.`label` = UPPER(NEW.`slug`);
            END""",
            """
                CREATE TRIGGER `tg_posts_insert` AFTER INSERT ON `posts` FOR EACH ROW BEGIN
                  IF (NOT (SELECT `r`.`active` FROM `authors` AS `r` WHERE `r`.`id` = NEW.`author_id`)) THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The author is not active.';
                  END IF;
                END""",
            """
                CREATE TRIGGER `tg_posts_update` AFTER UPDATE ON `posts` FOR EACH ROW BEGIN
                  IF (NOT (OLD.`title` <=> NEW.`title`)) THEN
                    INSERT INTO `post_events` (`post_id`, `title`) VALUES (NEW.`id`, OLD.`title`);
                  END IF;
                END""",
            """
                CREATE TRIGGER `tg_posts_delete` AFTER DELETE ON `posts` FOR EACH ROW BEGIN
                  IF (NOT (OLD.`revision` = 0)) THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Revised posts stay.';
                  END IF;
                END""");
    assertThat(new ScriptWriter(new MariaDbDialect(), after, before).write(after.changesTo(before)).statements())
        .containsExactly("DROP TRIGGER `rw_posts`",
            "DROP TRIGGER `tg_posts_insert`",
            "DROP TRIGGER `tg_posts_update`",
            "DROP TRIGGER `tg_posts_delete`");
  }
}
