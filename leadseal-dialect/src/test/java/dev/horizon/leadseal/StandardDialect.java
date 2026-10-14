package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.NowDefaultAttribute;
import dev.horizon.leadseal.command.CommandFactory;

final class StandardDialect implements Dialect {

  private final String name;
  private final CommandFactory commands = new CommandFactory();

  StandardDialect() {
    this("standard");
  }

  StandardDialect(String name) {
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
      case TextType text -> text.length() == null ? "TEXT" : "TEXT(" + text.length() + ")";
      case IntegerType integer -> integer.size().name();
      default -> column.type().getClass().getSimpleName().replace("Type", "").toUpperCase();
    };
  }

  @Override
  public String literal(Type type, String value) {
    return type instanceof TextType ? string(value) : value;
  }

  @Override
  public String generated(Column column, DefaultAttribute value) {
    return value instanceof NowDefaultAttribute ? "CURRENT_TIMESTAMP" : "gen_random_uuid()";
  }

  @Override
  public CommandFactory commands() {
    return commands;
  }
}
