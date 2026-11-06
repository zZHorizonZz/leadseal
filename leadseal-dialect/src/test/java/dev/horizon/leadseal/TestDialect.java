package dev.horizon.leadseal;

import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Collation;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.command.ColumnDdl;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommentDdl;
import dev.horizon.leadseal.command.ConstraintDdl;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.command.TableDdl;
import dev.horizon.leadseal.command.TriggerDdl;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlEnvironment;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.BytesType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.JsonType;
import dev.horizon.leadseal.type.Length;
import dev.horizon.leadseal.type.NumberType;
import dev.horizon.leadseal.type.TemporalType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimeType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class TestDialect implements Dialect {

  private final String name;
  private final Ddl ddl = new TestDdl(this);

  TestDialect() {
    this("test");
  }

  TestDialect(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String quote(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String string(String text) {
    return "'" + text.replace("'", "''") + "'";
  }

  @Override
  public String type(Table table, Column column) {
    return switch (column.type()) {
      case TextType(Length length) -> length.map(characters -> "TEXT(" + characters + ")", () -> "TEXT");
      case IntegerType(IntegerType.Size size) -> size.name();
      case BooleanType _ -> "BOOLEAN";
      case FloatType _ -> "FLOAT";
      case DecimalType _ -> "DECIMAL";
      case BytesType _ -> "BYTES";
      case UuidType _ -> "UUID";
      case TimestampType _ -> "TIMESTAMP";
      case DateType _ -> "DATE";
      case TimeType _ -> "TIME";
      case JsonType _ -> "JSON";
      case EnumType(_, _, EnumType.Storage storage) -> storage.select("TEXT", "INT");
    };
  }

  @Override
  public String literal(Type type, String value) {
    return switch (type) {
      case TextType _ -> string(value);
      case EnumType(_, _, EnumType.Storage storage) -> storage.select(string(value), value);
      case BooleanType _,NumberType _,BytesType _,UuidType _,TemporalType _,JsonType _ -> value;
    };
  }

  @Override
  public String currentDate() {
    return "CURRENT_DATE";
  }

  @Override
  public String currentTimestamp() {
    return "CURRENT_TIMESTAMP";
  }

  @Override
  public String randomUuid() {
    return "gen_random_uuid()";
  }

  @Override
  public Ddl ddl() {
    return ddl;
  }

  private record TestDdl(Dialect dialect) implements Ddl {

    @Override
    public TableDdl table() {
      return new TestTableDdl(this);
    }

    @Override
    public ColumnDdl column() {
      return new TestColumnDdl(this);
    }

    @Override
    public ConstraintDdl constraint() {
      return () -> this;
    }

    @Override
    public TriggerDdl trigger() {
      return new TestTriggerDdl(this);
    }

    @Override
    public CommentDdl comment() {
      return new TestCommentDdl(dialect);
    }
  }

  private record TestTriggerDdl(Ddl ddl) implements TriggerDdl {

    @Override
    public List<Command> createTriggers(TableTransition table, Triggers triggers) {
      SqlEnvironment environment = SqlEnvironment.of(ddl.dialect()).with(Row.CURRENT, "NEW").with(Row.NEW, "NEW")
          .with(Row.OLD, "OLD");
      return triggers.runs().stream().<Command> map(mutation -> script -> script.append("CREATE TRIGGER ")
          .name(triggers.name(table.name(), mutation)).append(" AFTER " + mutation.name() + " ON ").table(table.name())
          .append(" SET " + triggers.ordered().stream().map(rewrite -> "NEW." + ddl.dialect().quote(rewrite.column())
              + " = " + rewrite.expression().value(environment)).collect(Collectors.joining(", "))))
          .toList();
    }

    @Override
    public List<Command> dropTriggers(TableTransition table, Triggers triggers) {
      return triggers.runs().stream().<Command> map(mutation -> script -> script.append("DROP TRIGGER ")
          .name(triggers.name(table.name(), mutation))).toList();
    }
  }

  private record TestTableDdl(Ddl ddl) implements TableDdl {

    @Override
    public List<Command> moveTable(TableName table, Namespace.Name namespace) {
      String text = namespace.map(name -> name, () -> "public");
      List<Command> all = new ArrayList<>(createSchema(text));
      all.add(script -> script.append("ALTER TABLE ").table(table).append(" SET SCHEMA ").name(text));
      return all;
    }
  }

  private record TestColumnDdl(Ddl ddl) implements ColumnDdl {

    @Override
    public List<Command> collate(ColumnTransition column, Collation collation) {
      return alterColumnType(column);
    }
  }

  private record TestCommentDdl(Dialect dialect) implements CommentDdl {

    @Override
    public List<Command> commentColumn(ColumnTransition column) {
      String text = column.definition().attribute(Comment.class).map(comment -> dialect.string(comment.text()))
          .orElse("NULL");
      return List.of(script -> script.append("COMMENT ON COLUMN ").table(column.table().name()).append(".")
          .name(column.name()).append(" IS " + text));
    }

    @Override
    public List<Command> commentTable(TableTransition table) {
      String text = table.definition().attribute(Comment.class).map(comment -> dialect.string(comment.text()))
          .orElse("NULL");
      return List.of(script -> script.append("COMMENT ON TABLE ").table(table.name()).append(" IS " + text));
    }
  }
}
