package dev.horizon.leadseal.postgres;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.Default;
import dev.horizon.leadseal.attribute.GeneratedDefault;
import dev.horizon.leadseal.attribute.LiteralDefault;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Command.AlterColumn;
import dev.horizon.leadseal.command.TypeDdl;
import dev.horizon.leadseal.type.EnumType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The enum types of PostgreSQL. A value that is new at the end or between two values is added with
 * {@code ADD VALUE}, and a renamed value is renamed with {@code RENAME VALUE}. PostgreSQL cannot drop or reorder the
 * values of a type, and a transaction cannot use a value that it adds. So when values are dropped or reordered, or a
 * default or the zero of a column is a new value, the migration replaces the type: it renames the old type, creates
 * the new one, converts the columns through text, and drops the old type.
 */
record PostgresTypeDdl(PostgresDdl ddl) implements TypeDdl {

  private static final String ORIGIN = "leadseal_enum_origin";

  @Override
  public List<Command> createType(EnumType type) {
    return List.of(new CreateType(type));
  }

  @Override
  public List<Command> dropType(EnumType type) {
    return List.of(new DropType(type.name()));
  }

  @Override
  public List<Command> alterType(EnumType origin, EnumType target, Transition transition) {
    Dialect dialect = ddl.dialect();
    List<Command> all = new ArrayList<>();
    target.renames(origin).forEach(rename -> all.add(new AlterType(target.name(), "RENAME VALUE "
        + dialect.string(rename.origin()) + " TO " + dialect.string(rename.target()))));
    Map<Integer, String> targetNames = new HashMap<>();
    target.values().forEach(value -> targetNames.put(value.number(), value.name()));
    List<String> originNames = origin.values().stream()
        .map(value -> targetNames.getOrDefault(value.number(), value.name())).toList();
    List<String> names = target.values().stream().map(EnumType.Value::name).toList();
    Set<String> additions = names.stream().filter(name -> !originNames.contains(name)).collect(Collectors.toSet());
    if (names.stream().filter(originNames::contains).toList().equals(originNames) && !uses(transition, target,
        additions)) {
      int position = 0;
      for (String name : names) {
        if (position < originNames.size() && originNames.get(position).equals(name)) {
          position++;
        } else {
          all.add(new AlterType(target.name(), "ADD VALUE " + dialect.string(name) + (position < originNames.size()
              ? " BEFORE " + dialect.string(originNames.get(position))
              : "")));
        }
      }
      return all;
    }
    all.add(new AlterType(target.name(), "RENAME TO " + dialect.quote(ORIGIN)));
    all.add(new CreateType(target));
    for (TableName name : transition.origin().names()) {
      Optional<TableName> targetName = transition.targetName(name);
      Table table = transition.origin().table(name).orElseThrow();
      for (Column column : table.columns()) {
        if (targetName.isPresent() && column.type().equals(origin)) {
          all.addAll(convert(targetName.get(), column, target));
        }
      }
    }
    all.add(new DropType(ORIGIN));
    return all;
  }

  private List<Command> convert(TableName table, Column column, EnumType target) {
    Dialect dialect = ddl.dialect();
    Optional<Default> value = column.attribute(Default.class);
    List<Command> all = new ArrayList<>();
    value.ifPresent(_ -> all.add(new AlterColumn(table, column.name(), "DROP DEFAULT")));
    all.add(new AlterColumn(table, column.name(), "TYPE " + dialect.quote(target.name()) + " USING "
        + dialect.quote(column.name()) + "::text::" + dialect.quote(target.name())));
    value.filter(columnDefault -> switch (columnDefault) {
      case LiteralDefault literal -> target.accepts(literal.value());
      case GeneratedDefault _ -> true;
    }).ifPresent(columnDefault -> all.add(new AlterColumn(table, column.name(), "SET DEFAULT " + dialect.defaultValue(
        column, columnDefault))));
    return all;
  }

  private boolean uses(Transition transition, EnumType target, Set<String> names) {
    return transition.target().names().stream().flatMap(name -> transition.target().table(name).orElseThrow()
        .columns().stream()).filter(column -> column.type().equals(target))
        .anyMatch(column -> names.contains(target.zero()) || names.stream().anyMatch(name -> column
            .attribute(Default.class).equals(Optional.of(new LiteralDefault(name)))));
  }

  private record CreateType(EnumType type) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE TYPE ").name(type.name()).append(" AS ENUM (" + type.values().stream()
          .map(value -> script.dialect().string(value.name())).collect(Collectors.joining(", ")) + ")");
    }
  }

  private record AlterType(String name, String action) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TYPE ").name(name).append(" " + action);
    }
  }

  private record DropType(String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP TYPE ").name(name);
    }
  }
}
