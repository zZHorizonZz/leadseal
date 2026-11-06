package dev.horizon.leadseal.sqlserver;

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

class SqlServerDialectTest {

  @Test
  @DisplayName("Rejects identity changes of existing columns")
  void rejectIdentityChange() {
    Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNull.INSTANCE);
    PrimaryKey key = PrimaryKey.of("orders", List.of("id"));
    Schema before = Schema.of(Table.of("o", "orders", List.of(id), key));
    Schema after = Schema.of(Table.of("o", "orders", List.of(id.with(Identity.INSTANCE)), key));

    assertThatThrownBy(() -> new ScriptWriter(new SqlServerDialect(), before, after)
        .write(before.changesTo(after))).isInstanceOf(UnsupportedChangeException.class)
        .hasMessageContaining("cannot add IDENTITY");
  }

  @Test
  @DisplayName("Separates the batch of an isolated statement")
  void separateIsolatedBatch() {
    ScriptWriter sql = new ScriptWriter(new SqlServerDialect(), Schema.empty(), Schema.empty());
    sql.append("SELECT 1").end().append("SELECT 2").end().isolate().append("CREATE TRIGGER t").end()
        .append("SELECT 3").end().isolate().append("CREATE TRIGGER u").end();

    assertThat(sql.text()).isEqualTo("SELECT 1;\n\nSELECT 2;\nGO\n\nCREATE TRIGGER t;\nGO\n\nSELECT 3;\nGO\n\n"
        + "CREATE TRIGGER u;\n");
    assertThat(sql.statements()).containsExactly("SELECT 1", "SELECT 2", "CREATE TRIGGER t", "SELECT 3",
        "CREATE TRIGGER u");
  }

  @Nested
  class Expressions extends DialectExpressionTest {

    @Override
    protected Dialect dialect() {
      return new SqlServerDialect();
    }

    @Override
    protected String sql() {
      return """
          PAID_CONDITION                 | (NEW.[paid] = 1)
          NOT_PAID_CONDITION             | (NOT (NEW.[paid] = 1))
          POSITIVE_CONDITION             | (NEW.[quantity] > 2)
          POSITIVE_VALUE                 | CASE WHEN (NEW.[quantity] > 2) THEN 1 ELSE 0 END
          POSITIVE_EQUALS_PAID_CONDITION | (CASE WHEN (NEW.[quantity] > 2) THEN 1 ELSE 0 END = NEW.[paid])
          TRUE_CONDITION                 | (1 = 1)
          CONCAT_VALUE                   | (NEW.[name] + N'!')
          INTEGER_DIVIDE_VALUE           | (NEW.[quantity] / 2)
          LENGTH_VALUE                   | (LEN(NEW.[name] + N'x') - 1)
          CHANGE_CONDITION               | ((OLD.[name] <> NEW.[name]) OR (OLD.[name] IS NULL AND NEW.[name] IS NOT NULL) OR (OLD.[name] IS NOT NULL AND NEW.[name] IS NULL))
          NOW_VALUE                      | SYSUTCDATETIME()
          UUID_VALUE                     | NEWID()
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

    assertThat(new ScriptWriter(new SqlServerDialect(), before, after).write(before.changesTo(after)).statements())
        .containsExactly(
            """
                CREATE TRIGGER [tg_posts_insert] ON [posts] AFTER INSERT AS
                BEGIN
                  SET NOCOUNT ON;
                  IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;
                  IF EXISTS (SELECT 1 FROM [posts] AS [t] JOIN inserted AS [i] ON [t].[id] = [i].[id] WHERE (NOT ((SELECT [r].[active] FROM [authors] AS [r] WHERE [r].[id] = [t].[author_id]) = 1))) THROW 50000, N'The author is not active.', 1;
                END""",
            """
                CREATE TRIGGER [tg_posts_update] ON [posts] AFTER UPDATE AS
                BEGIN
                  SET NOCOUNT ON;
                  IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;
                  UPDATE [t] SET [slug] = LOWER([t].[title]), [revision] = ([d].[revision] + 1) FROM [posts] AS [t] JOIN inserted AS [i] ON [t].[id] = [i].[id] LEFT JOIN deleted AS [d] ON [t].[id] = [d].[id];
                  UPDATE [t] SET [label] = UPPER([t].[slug]) FROM [posts] AS [t] JOIN inserted AS [i] ON [t].[id] = [i].[id] LEFT JOIN deleted AS [d] ON [t].[id] = [d].[id];
                  INSERT INTO [post_events] ([post_id], [title]) SELECT [t].[id], [d].[title] FROM [posts] AS [t] JOIN inserted AS [i] ON [t].[id] = [i].[id] LEFT JOIN deleted AS [d] ON [t].[id] = [d].[id] WHERE (([d].[title] <> [t].[title]) OR ([d].[title] IS NULL AND [t].[title] IS NOT NULL) OR ([d].[title] IS NOT NULL AND [t].[title] IS NULL));
                END""",
            """
                CREATE TRIGGER [tg_posts_delete] ON [posts] AFTER DELETE AS
                BEGIN
                  SET NOCOUNT ON;
                  IF TRIGGER_NESTLEVEL(@@PROCID) > 1 RETURN;
                  IF EXISTS (SELECT 1 FROM deleted AS [d] WHERE (NOT ([d].[revision] = 0))) THROW 50000, N'Revised posts stay.', 1;
                END""");
    assertThat(new ScriptWriter(new SqlServerDialect(), after, before).write(after.changesTo(before)).statements())
        .containsExactly("DROP TRIGGER [tg_posts_insert]",
            "DROP TRIGGER [tg_posts_update]",
            "DROP TRIGGER [tg_posts_delete]");
  }
}
