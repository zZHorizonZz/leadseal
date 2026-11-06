package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.attribute.Rewrite;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.TextType;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class RewriteContractTest extends ContractTest {

  private static final Column ID = column("1", "id", BIGINT);
  private static final Column SLUG = nullable("3", "slug", TEXT);
  private static final Column LABEL = nullable("4", "label", TextType.of(50));
  private static final Column REVISION = column("5", "revision", INT).with(new LiteralDefault("0"));
  private static final Column MODIFIED = nullable("6", "modified", TIMESTAMP);

  private static Column title(String name) {
    return column("2", name, TEXT);
  }

  private static SqlExpression call(Call.Function function, SqlExpression argument) {
    return new Call(function, List.of(argument));
  }

  private static List<Rewrite> rules(String title) {
    return List.of(new Rewrite("slug", call(Call.Function.LOWER, ColumnReference.of(title))),
        new Rewrite("label", call(Call.Function.UPPER, ColumnReference.of("slug"))),
        new Rewrite("revision", new Arithmetic(Arithmetic.Operator.ADD, new ColumnReference("revision", Row.OLD),
            new Literal(INT, "1"))),
        new Rewrite("modified", new Call(Call.Function.NOW, List.of())));
  }

  private static Table posts(String name, String title, Column label, List<Rewrite> rules) {
    Table table = table("blog.Post", name, List.of(ID, title(title), SLUG, label, REVISION, MODIFIED), "id");
    return rules.isEmpty() ? table : table.with(new Triggers(rules, List.of()));
  }

  private static Schema posts(List<Rewrite> rules) {
    return schema(posts("posts", "title", LABEL, rules));
  }

  private void insert(String table, long id, String title) throws SQLException {
    execute("INSERT INTO " + q(table) + " (" + q("id") + ", " + q("title") + ") VALUES (?, ?)", id, title);
  }

  private void rename(String table, String column, String title) throws SQLException {
    execute("UPDATE " + q(table) + " SET " + q(column) + " = ?", title);
  }

  private List<List<String>> rows(String table, String... columns) throws SQLException {
    return query("SELECT " + String.join(", ", Arrays.stream(columns).map(this::q).toList()) + " FROM " + q(table)
        + " ORDER BY " + q("id"));
  }

  @Test
  @DisplayName("Writes no rewrite on an insert, which gives the columns their defaults")
  void insertDefaults() throws SQLException {
    apply(posts(rules("title")));

    insert("posts", 1, "Hello World");

    assertThat(rows("posts", "slug", "label", "revision", "modified")).containsExactly(Arrays.asList(null, null, "0",
        null));
  }

  @Test
  @DisplayName("Writes the rules on every updated row, each after the rules of the columns that it reads")
  void rewriteUpdate() throws SQLException {
    apply(posts(rules("title")));
    insert("posts", 1, "First");
    insert("posts", 2, "Second");

    rename("posts", "title", "Changed Title");

    assertThat(rows("posts", "slug", "label", "revision")).containsExactly(
        List.of("changed title", "CHANGED TITLE", "1"), List.of("changed title", "CHANGED TITLE", "1"));
    assertThat(count("SELECT COUNT(*) FROM " + q("posts") + " WHERE " + q("modified") + " IS NOT NULL")).isEqualTo(2);
  }

  @Test
  @DisplayName("Replaces the value that the update writes into a rewritten column")
  void replaceValue() throws SQLException {
    apply(posts(rules("title")));
    insert("posts", 1, "First");

    rename("posts", "slug", "custom");

    assertThat(rows("posts", "slug", "revision")).containsExactly(List.of("first", "1"));
  }

  @Test
  @DisplayName("Runs the changed rules after a migration changes them")
  void changeRule() throws SQLException {
    apply(posts(rules("title")));
    insert("posts", 1, "First");

    apply(posts(List.of(new Rewrite("slug", call(Call.Function.UPPER, ColumnReference.of("title"))))));
    rename("posts", "title", "Changed");

    assertThat(rows("posts", "slug", "revision")).containsExactly(List.of("CHANGED", "0"));
  }

  @Test
  @DisplayName("Keeps the rewrites working after a migration renames a column that they read")
  void renameColumn() throws SQLException {
    apply(posts(rules("title")));
    insert("posts", 1, "First");

    apply(schema(posts("posts", "heading", LABEL, rules("heading"))));
    rename("posts", "heading", "Changed");

    assertThat(rows("posts", "slug", "revision")).containsExactly(List.of("changed", "1"));
  }

  @Test
  @DisplayName("Keeps the rewrites working after a migration renames the table, and drops them later")
  void renameTable() throws SQLException {
    apply(posts(rules("title")));
    insert("posts", 1, "First");

    apply(schema(posts("articles", "title", LABEL, rules("title"))));
    rename("articles", "title", "Second");
    apply(schema(posts("articles", "title", LABEL, List.of())));
    rename("articles", "title", "Third");

    assertThat(rows("articles", "slug", "revision")).containsExactly(List.of("second", "1"));
  }

  @Test
  @DisplayName("Drops the rewrites with their table, so a new table can take its name")
  void dropTable() throws SQLException {
    apply(posts(rules("title")));
    apply(schema());

    apply(posts(rules("title")));
    insert("posts", 1, "First");
    rename("posts", "title", "Changed");

    assertThat(rows("posts", "slug")).containsExactly(List.of("changed"));
  }

  @Test
  @DisplayName("Keeps the rewrites working after a migration changes the type of a column")
  void changeType() throws SQLException {
    apply(posts(rules("title")));
    insert("posts", 1, "First");

    apply(schema(posts("posts", "title", nullable("4", "label", TEXT), rules("title"))));
    rename("posts", "title", "Changed");

    assertThat(rows("posts", "slug", "label")).containsExactly(List.of("changed", "CHANGED"));
  }
}
