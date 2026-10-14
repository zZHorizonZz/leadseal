package dev.horizon.leadseal;

final class StandardDialect implements Dialect {

  private final String name;

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
  public String type(Table table, Column column) {
    return column.type().name() + (column.length() == null ? "" : "(" + column.length() + ")");
  }

  @Override
  public String string(String text) {
    return "'" + text.replace("'", "''") + "'";
  }

  @Override
  public String literal(Column column, String value) {
    return switch (column.type()) {
      case BOOLEAN, INT, BIGINT, REAL, DOUBLE, DECIMAL -> value;
      default -> string(value);
    };
  }

  @Override
  public String generated(Column column, Default.Kind kind) {
    return kind == Default.Kind.NOW ? "CURRENT_TIMESTAMP" : "gen_random_uuid()";
  }

  @Override
  public Decorator decorator() {
    return Decorator.none();
  }
}
