package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Comment;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommentDdl;
import java.util.ArrayList;
import java.util.List;

record SqlServerCommentDdl(Dialect dialect) implements CommentDdl {

  @Override
  public List<Command> commentColumn(ColumnTransition column) {
    Description description = new Description(column.table().name(), ", @level2type = N'COLUMN', @level2name = "
        + dialect.string(column.name()));
    List<Command> all = new ArrayList<>();
    column.origin().flatMap(origin -> origin.attribute(Comment.class))
        .ifPresent(_ -> all.add(new DropDescription(description)));
    column.definition().attribute(Comment.class)
        .ifPresent(comment -> all.add(new AddDescription(description, dialect.string(comment.text()))));
    return all;
  }

  @Override
  public List<Command> commentTable(TableTransition table) {
    Description description = new Description(table.name(), "");
    List<Command> all = new ArrayList<>();
    table.origin().flatMap(origin -> origin.attribute(Comment.class))
        .ifPresent(_ -> all.add(new DropDescription(description)));
    table.definition().attribute(Comment.class)
        .ifPresent(comment -> all.add(new AddDescription(description, dialect.string(comment.text()))));
    return all;
  }

  private record Description(TableName table, String level) {

    String declare(ScriptWriter script, SqlServerVariable schema) {
      String value = table.namespace().map(script.dialect()::string, () -> "SCHEMA_NAME()");
      return "DECLARE " + schema.name() + " sysname = " + value;
    }

    String target(ScriptWriter script, SqlServerVariable schema) {
      return "@level0type = N'SCHEMA', @level0name = " + schema.name() + ", @level1type = N'TABLE', @level1name = "
          + script.dialect().string(table.name()) + level;
    }
  }

  private record AddDescription(Description description, String text) implements Command {

    @Override
    public void render(ScriptWriter script) {
      SqlServerVariable schema = SqlServerVariable.of(script, "schema");
      script.append(description.declare(script, schema) + " EXEC sp_addextendedproperty @name = N'MS_Description',"
          + " @value = " + text + ", " + description.target(script, schema));
    }
  }

  private record DropDescription(Description description) implements Command {

    @Override
    public void render(ScriptWriter script) {
      SqlServerVariable schema = SqlServerVariable.of(script, "schema");
      script.append(description.declare(script, schema) + " EXEC sp_dropextendedproperty @name = N'MS_Description', "
          + description.target(script, schema));
    }
  }
}
