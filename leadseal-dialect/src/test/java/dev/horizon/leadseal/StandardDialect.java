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
  public String zero(Column column) {
    return column.type() == ColumnType.TEXT ? "''" : "0";
  }

  @Override
  public Decorator decorator() {
    return Decorator.none();
  }
}
