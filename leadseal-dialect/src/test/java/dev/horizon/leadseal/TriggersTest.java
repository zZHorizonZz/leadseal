package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Identity;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.change.AddTableAttribute;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateTable;
import dev.horizon.leadseal.change.DropTable;
import dev.horizon.leadseal.change.DropTableAttribute;
import dev.horizon.leadseal.change.RenameColumn;
import dev.horizon.leadseal.change.RenameTable;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.ForeignColumn;
import dev.horizon.leadseal.expression.IsNull;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimestampType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TriggersTest {

  private static final IntegerType BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final Column ID = Column.of("1", "id", BIGINT, NotNull.INSTANCE);
  private static final Column TITLE = Column.of("2", "title", TextType.unlimited(), NotNull.INSTANCE);
  private static final Column SLUG = Column.of("3", "slug", TextType.unlimited());
  private static final Column LABEL = Column.of("4", "label", TextType.unlimited());
  private static final Column MODIFIED = Column.of("5", "modified", new TimestampType());
  private static final Column AUTHOR = Column.of("6", "author_id", BIGINT);
  private static final TableName POSTS = TableName.of("posts");
  private static final TableName EVENTS = TableName.of("post_events");
  private static final Rewrite SLUG_OF_TITLE = new Rewrite("slug", lower(ColumnReference.of("title")));
  private static final Rewrite LABEL_OF_SLUG = new Rewrite("label", upper(ColumnReference.of("slug")));
  private static final Rewrite MODIFIED_NOW = new Rewrite("modified", new Call(Call.Function.NOW, List.of()));
  private static final Trigger LOG = new Trigger("log", List.of(Mutation.UPDATE), new ColumnChange("title"),
      List.of(new Trigger.Insert(EVENTS, List.of(new Trigger.Assignment("post_id", ColumnReference.of("id"))))));

  private static SqlExpression lower(SqlExpression text) {
    return new Call(Call.Function.LOWER, List.of(text));
  }

  private static SqlExpression upper(SqlExpression text) {
    return new Call(Call.Function.UPPER, List.of(text));
  }

  private static Table posts(Column... columns) {
    return Table.of("blog.Post", "posts", List.of(columns), PrimaryKey.of("posts", List.of("id")));
  }

  private static Table posts(Triggers triggers) {
    return posts(ID, TITLE, SLUG, LABEL, MODIFIED).with(triggers);
  }

  private static Table events(Column... others) {
    return Table.of("blog.PostEvent", "post_events", Stream.concat(Stream.of(ID.with(Identity.INSTANCE),
        Column.of("2", "post_id", BIGINT, NotNull.INSTANCE)), Stream.of(others)).toList(), PrimaryKey.of(
            "post_events", List.of("id")));
  }

  private static Trigger assertion(String name, List<Mutation> on, SqlExpression condition) {
    return new Trigger(name, on, Trigger.ALWAYS, List.of(new Trigger.Assert(condition, "refused")));
  }

  @Test
  @DisplayName("Runs a rewrite after the rewrites of the columns that it reads")
  void orderRewrites() {
    Triggers triggers = Triggers.of(LABEL_OF_SLUG, SLUG_OF_TITLE, MODIFIED_NOW);

    assertThat(triggers.levels()).containsExactly(List.of(SLUG_OF_TITLE, MODIFIED_NOW), List.of(LABEL_OF_SLUG));
    assertThat(triggers.ordered()).containsExactly(SLUG_OF_TITLE, MODIFIED_NOW, LABEL_OF_SLUG);
  }

  @Test
  @DisplayName("Keeps a rewrite that reads its own column or an old value in the first level")
  void readOwnColumn() {
    Rewrite slug = new Rewrite("slug", lower(ColumnReference.of("slug")));
    Rewrite label = new Rewrite("label", upper(new ColumnReference("slug", Row.OLD)));

    assertThat(Triggers.of(slug, label).levels()).containsExactly(List.of(slug, label));
  }

  @Test
  @DisplayName("Names a database trigger for the rewrites and one for each mutation with triggers")
  void nameTriggers() {
    Triggers triggers = new Triggers(List.of(SLUG_OF_TITLE), List.of(LOG, assertion("keep", List.of(Mutation.DELETE),
        Trigger.ALWAYS)));

    assertThat(triggers.rewriteName(POSTS)).isEqualTo("rw_posts");
    assertThat(triggers.name(POSTS, Mutation.UPDATE)).isEqualTo("tg_posts_update");
    assertThat(triggers.mutations()).containsExactly(Mutation.UPDATE, Mutation.DELETE);
    assertThat(Triggers.of(SLUG_OF_TITLE).runs()).containsExactly(Mutation.UPDATE);
  }

  @Test
  @DisplayName("Gives the triggers of a mutation in their order")
  void orderTriggers() {
    Trigger first = assertion("first", List.of(Mutation.INSERT, Mutation.UPDATE), Trigger.ALWAYS);
    Trigger second = assertion("second", List.of(Mutation.UPDATE), Trigger.ALWAYS);

    assertThat(new Triggers(List.of(), List.of(first, second)).triggers(Mutation.UPDATE)).containsExactly(first,
        second);
  }

  static Stream<Arguments> invalidTables() {
    return Stream.of(
        arguments("rewrites that read each other", posts(ID, TITLE, SLUG, LABEL), Triggers.of(new Rewrite("slug", lower(
            ColumnReference.of("label"))), LABEL_OF_SLUG), "read each other: slug -> label -> slug"),
        arguments("a rewrite of the primary key", posts(ID, TITLE), Triggers.of(new Rewrite("id", ColumnReference.of(
            "id"))), "part of the primary key"),
        arguments("a rewrite of a column the table does not have", posts(ID, TITLE), Triggers.of(new Rewrite("slug",
            ColumnReference.of("title"))), "which the table does not have"),
        arguments("an unknown column in a rewrite", posts(ID, TITLE, SLUG), Triggers.of(new Rewrite("slug",
            ColumnReference.of("name"))), "reads the column name"),
        arguments("the new row in a delete", posts(ID, TITLE), new Triggers(List.of(), List.of(assertion("keep", List.of(
            Mutation.DELETE), new ColumnReference("title", Row.NEW)))), "new row"),
        arguments("the new row in a delete of another table", posts(ID, TITLE), new Triggers(List.of(), List.of(
            action("purge", Mutation.DELETE, new Trigger.Delete(EVENTS, new Comparison(Comparison.Operator.EQUALS,
                ColumnReference.of("post_id"), new ColumnReference("id", Row.NEW)))))),
            "new row"),
        arguments("the old row in an insert and a delete", posts(ID, TITLE), new Triggers(List.of(), List.of(assertion(
            "keep", List.of(Mutation.INSERT, Mutation.DELETE), new ColumnReference("title", Row.OLD)))), "old row"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidTables")
  @DisplayName("Refuses rewrites and triggers that do not fit their table")
  void refuseTable(String reason, Table table, Triggers triggers, String message) {
    assertThatThrownBy(() -> table.with(triggers)).isInstanceOf(InvalidSchemaException.class).hasMessageContaining(
        message);
  }

  static Stream<Arguments> invalidSchemas() {
    Trigger own = new Trigger("own", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Insert(POSTS, List.of(
        new Trigger.Assignment("title", ColumnReference.of("title"))))));
    Trigger missing = new Trigger("log", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Insert(EVENTS, List.of(
        new Trigger.Assignment("note", ColumnReference.of("title"))))));
    Trigger back = new Trigger("back", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Insert(POSTS, List.of(
        new Trigger.Assignment("id", ColumnReference.of("id")), new Trigger.Assignment("title", ColumnReference.of(
            "id"))))));
    Trigger author = assertion("author", List.of(Mutation.INSERT), new ForeignColumn(ColumnReference.of("author_id"),
        TableName.of("authors"), "id", "active"));
    return Stream.of(
        arguments("an insert into its own table", List.of(posts(ID, TITLE).with(new Triggers(List.of(), List.of(
            own)))), "writes into its own table"),
        arguments("an insert into a table the schema does not have", List.of(posts(ID, TITLE).with(new Triggers(List
            .of(), List.of(LOG)))), "which the schema does not have"),
        arguments("an insert into a column the other table does not have", List.of(posts(ID, TITLE).with(
            new Triggers(List.of(), List.of(missing))), events()), "post_events.note, which the table does not have"),
        arguments("an insert without a NOT NULL column", List.of(posts(ID, TITLE).with(new Triggers(List.of(), List
            .of(new Trigger("log", List.of(Mutation.INSERT), Trigger.ALWAYS, List.of(new Trigger.Insert(EVENTS, List.of(
                new Trigger.Assignment("post_id", ColumnReference.of("id"))))))))),
            events(Column.of("3", "kind",
                TextType.unlimited(), NotNull.INSTANCE))),
            "without the column kind"),
        arguments("triggers that write in a loop", List.of(posts(ID, TITLE).with(new Triggers(List.of(), List.of(
            LOG))), events().with(new Triggers(List.of(), List.of(back)))),
            "write in a loop: posts.log -> post_events.back -> posts"),
        arguments("a read of a table the schema does not have", List.of(posts(ID, TITLE, AUTHOR).with(new Triggers(
            List.of(), List.of(author)))), "reads the table authors"),
        arguments("an update of its own table", List.of(posts(ID, TITLE).with(new Triggers(List.of(), List.of(
            action("touch", Mutation.INSERT, new Trigger.Update(POSTS, Trigger.ALWAYS, List.of(new Trigger.Assignment(
                "title", ColumnReference.of("title"))))))))),
            "writes into its own table"),
        arguments("an update of a column the other table does not have", List.of(posts(ID, TITLE).with(new Triggers(
            List.of(), List.of(action("touch", Mutation.INSERT, new Trigger.Update(EVENTS, Trigger.ALWAYS, List.of(
                new Trigger.Assignment("note", new ColumnReference("id", Row.NEW)))))))),
            events()),
            "changes the column post_events.note"),
        arguments("a delete that reads a column the other table does not have", List.of(posts(ID, TITLE).with(
            new Triggers(List.of(), List.of(action("purge", Mutation.DELETE, new Trigger.Delete(EVENTS,
                new Comparison(Comparison.Operator.EQUALS, ColumnReference.of("missing"), new ColumnReference("id",
                    Row.OLD))))))),
            events()), "reads the column missing"),
        arguments("an update that writes back in a loop", List.of(posts(ID, TITLE).with(new Triggers(List.of(), List.of(
            LOG))), events().with(new Triggers(List.of(),
                List.of(action("touch", Mutation.INSERT, new Trigger.Update(
                    POSTS, Trigger.ALWAYS, List.of(new Trigger.Assignment("title", new ColumnReference("title",
                        Row.CURRENT))))))))),
            "write in a loop: posts.log -> post_events.touch -> posts"));
  }

  private static Trigger action(String name, Mutation mutation, Trigger.Action action) {
    return new Trigger(name, List.of(mutation), Trigger.ALWAYS, List.of(action));
  }

  private static Trigger.Update update(String column) {
    return new Trigger.Update(EVENTS, new Comparison(Comparison.Operator.EQUALS, ColumnReference.of("post_id"),
        new ColumnReference("id", Row.NEW)), List.of(new Trigger.Assignment(column, ColumnReference.of(column))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidSchemas")
  @DisplayName("Refuses triggers that do not fit the other tables of the schema")
  void refuseSchema(String reason, List<Table> tables, String message) {
    assertThatThrownBy(() -> Schema.of(tables.toArray(Table[]::new))).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining(message);
  }

  @Test
  @DisplayName("Refuses two rewrites of a column and two triggers with one name")
  void refuseTwice() {
    assertThatThrownBy(() -> Triggers.of(SLUG_OF_TITLE, SLUG_OF_TITLE)).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining("two rewrites");
    assertThatThrownBy(() -> new Triggers(List.of(), List.of(LOG, LOG))).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining("two triggers named log");
  }

  @Test
  @DisplayName("Writes into every table that the actions of a trigger write into, in their order")
  void writeTables() {
    Trigger trigger = new Trigger("log", List.of(Mutation.UPDATE), Trigger.ALWAYS, List.of(update("post_id"),
        assertion("keep", List.of(Mutation.UPDATE), Trigger.ALWAYS).actions().getFirst(), LOG.actions().getFirst()));

    assertThat(new Triggers(List.of(), List.of(trigger)).writes()).containsExactly(EVENTS);
    assertThat(trigger.actions()).extracting(action -> action.writes().isPresent()).containsExactly(true, false, true);
    assertThat(update("post_id").reads()).containsExactly("id");
    assertThat(update("post_id").columns()).containsExactly("post_id");
  }

  @Test
  @DisplayName("Refuses a trigger without a mutation, and an assert without a message")
  void refuseTrigger() {
    assertThatThrownBy(() -> new Trigger("none", List.of(), Trigger.ALWAYS, LOG.actions()))
        .isInstanceOf(InvalidSchemaException.class).hasMessageContaining("runs on no mutation");
    assertThatThrownBy(() -> new Trigger.Assert(Trigger.ALWAYS, " ")).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining("no message");
  }

  static Stream<Arguments> changes() {
    Triggers triggers = new Triggers(List.of(SLUG_OF_TITLE, MODIFIED_NOW), List.of(LOG));
    Triggers renamed = new Triggers(List.of(new Rewrite("slug", lower(ColumnReference.of("name"))), MODIFIED_NOW),
        List.of(new Trigger("log", List.of(Mutation.UPDATE), new ColumnChange("name"), LOG.actions())));
    Triggers smaller = Triggers.of(SLUG_OF_TITLE);
    Table logged = posts(triggers);
    Table named = posts(ID, TITLE.withName("name"), SLUG, LABEL, MODIFIED).with(renamed);
    Table articles = Table.of("blog.Post", "articles", logged.columns(), PrimaryKey.of("articles", List.of("id")))
        .with(triggers);
    Table events = events();
    Table moved = Table.of("blog.PostEvent", "events", events.columns(), PrimaryKey.of("events", List.of("id")));
    Triggers toMoved = new Triggers(triggers.rewrites(), List.of(new Trigger("log", List.of(Mutation.UPDATE),
        new ColumnChange("title"), List.of(new Trigger.Insert(TableName.of("events"), List.of(new Trigger.Assignment("post_id",
            ColumnReference.of("id"))))))));
    return Stream.of(
        arguments("create a table with triggers", Schema.of(events), Schema.of(logged, events), List.of(new CreateTable(
            Namespace.Name.DEFAULT, logged.without(Triggers.class)), new AddTableAttribute(POSTS, triggers))),
        arguments("drop a table with triggers", Schema.of(logged, events), Schema.of(events), List.of(
            new DropTableAttribute(POSTS, triggers), new DropTable(POSTS))),
        arguments("change the triggers", Schema.of(logged, events), Schema.of(posts(smaller), events), List.of(
            new DropTableAttribute(POSTS, triggers), new AddTableAttribute(POSTS, smaller))),
        arguments("rename a column that they read", Schema.of(logged, events), Schema.of(named, events), List.of(
            new RenameColumn(POSTS, "title", "name"), new DropTableAttribute(POSTS, triggers), new AddTableAttribute(
                POSTS, renamed))),
        arguments("rename the table", Schema.of(logged, events), Schema.of(articles, events), List.of(new RenameTable(
            POSTS, "articles"), new DropTableAttribute(POSTS, triggers),
            new AddTableAttribute(TableName.of(
                "articles"), triggers))),
        arguments("rename the table that a trigger writes into", Schema.of(logged, events), Schema.of(posts(toMoved),
            moved),
            List.of(new RenameTable(EVENTS, "events"), new DropTableAttribute(POSTS, triggers),
                new AddTableAttribute(POSTS, toMoved))),
        arguments("keep the triggers", Schema.of(logged, events), Schema.of(posts(triggers), events), List.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("changes")
  @DisplayName("Drops the database triggers and creates them again when what they read or write changes")
  void findChanges(String change, Schema origin, Schema target, List<Change> expected) {
    assertThat(origin.changesTo(target)).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  @DisplayName("Creates the triggers again when a migration renames a column of the table that an update reads")
  void renameTargetColumn() {
    Triggers origin = new Triggers(List.of(), List.of(action("touch", Mutation.INSERT, update("post_id"))));
    Triggers target = new Triggers(List.of(), List.of(action("touch", Mutation.INSERT, new Trigger.Update(EVENTS,
        new Comparison(Comparison.Operator.EQUALS, ColumnReference.of("article_id"), new ColumnReference("id",
            Row.NEW)),
        List.of(new Trigger.Assignment("article_id", ColumnReference.of("article_id")))))));
    Table events = events();
    Table renamed = events.withColumns(List.of(events.columns().get(0), events.columns().get(1).withName("article_id")));

    assertThat(Schema.of(posts(origin), events).changesTo(Schema.of(posts(target), renamed)))
        .containsExactlyInAnyOrder(new RenameColumn(EVENTS, "post_id", "article_id"), new DropTableAttribute(POSTS,
            origin), new AddTableAttribute(POSTS, target));
  }

  @Test
  @DisplayName("Creates the triggers again when a migration renames a column of a row that they read through a key")
  void renameForeignColumn() {
    Column parent = Column.of("3", "parent_id", BIGINT);
    Triggers origin = new Triggers(List.of(), List.of(assertion("titled", List.of(Mutation.INSERT), new IsNull(
        new ForeignColumn(ColumnReference.of("parent_id"), TableName.of("nodes"), "id", "title")).negate())));
    Triggers target = new Triggers(List.of(), List.of(assertion("titled", List.of(Mutation.INSERT), new IsNull(
        new ForeignColumn(ColumnReference.of("parent_id"), TableName.of("nodes"), "id", "name")).negate())));
    Table nodes = Table.of("tree.Node", "nodes", List.of(ID, TITLE, parent), PrimaryKey.of("nodes", List.of("id")));
    Table renamed = Table.of("tree.Node", "nodes", List.of(ID, TITLE.withName("name"), parent), PrimaryKey.of("nodes",
        List.of("id")));

    assertThat(Schema.of(nodes.with(origin)).changesTo(Schema.of(renamed.with(target)))).contains(
        new DropTableAttribute(TableName.of("nodes"), origin), new AddTableAttribute(TableName.of("nodes"), target));
  }

  @Test
  @DisplayName("Creates the triggers again when a migration changes the type of the primary key")
  void changeKeyType() {
    Triggers triggers = new Triggers(List.of(), List.of(LOG));
    Table narrow = posts(Column.of("1", "id", new IntegerType(IntegerType.Size.INT32), NotNull.INSTANCE), TITLE).with(
        triggers);

    assertThat(Schema.of(narrow, events()).changesTo(Schema.of(posts(ID, TITLE).with(triggers), events())))
        .contains(new DropTableAttribute(POSTS, triggers), new AddTableAttribute(POSTS, triggers));
  }

  @Test
  @DisplayName("Refuses to read a row through a column that is not unique")
  void refuseLooseKey() {
    Table authors = Table.of("blog.Author", "authors", List.of(ID, Column.of("2", "email", TextType.of(100),
        NotNull.INSTANCE), Column.of("3", "active", new BooleanType(), NotNull.INSTANCE)), PrimaryKey.of("authors",
            List.of("id")));
    Table posts = posts(ID, TITLE, AUTHOR).with(new Triggers(List.of(), List.of(assertion("active", List.of(
        Mutation.INSERT),
        new ForeignColumn(ColumnReference.of("author_id"), TableName.of("authors"), "email",
            "active")))));

    assertThatThrownBy(() -> Schema.of(authors, posts)).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining("can match many rows");
  }

  @Test
  @DisplayName("Reads a column of the row that a foreign key references")
  void readForeignRow() {
    Table authors = Table.of("blog.Author", "authors", List.of(ID, Column.of("2", "active", new BooleanType(),
        NotNull.INSTANCE)), PrimaryKey.of("authors", List.of("id")));
    ForeignColumn active = new ForeignColumn(ColumnReference.of("author_id"), TableName.of("authors"), "id", "active");
    Table posts = posts(ID, TITLE, AUTHOR).with(ForeignKey.of("posts", List.of("author_id"), TableName.of("authors"),
        List.of("id"), ForeignKey.Action.NO_ACTION)).with(new Triggers(List.of(),
            List.of(assertion("active", List.of(
                Mutation.INSERT), active))));

    assertThat(Schema.of(authors, posts).changesTo(Schema.of(authors, posts))).isEmpty();
    assertThat(active.columns()).containsExactly(ColumnReference.of("author_id"));
  }

  @Test
  @DisplayName("Drops triggers before every other change and creates them after")
  void orderPhases() {
    assertThat(Change.Phase.DROP_TRIGGERS.precedes(Change.Phase.DROP_FOREIGN_KEYS)).isTrue();
    assertThat(Change.Phase.ADD_FOREIGN_KEYS.precedes(Change.Phase.ADD_TRIGGERS)).isTrue();
  }
}
