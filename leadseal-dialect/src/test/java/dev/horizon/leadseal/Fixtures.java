package dev.horizon.leadseal;

import java.util.ArrayList;
import java.util.List;

public final class Fixtures {

  private Fixtures() {
  }

  public static Column column(String id, String name, ColumnType type) {
    return new Column(id, name, type, null, false);
  }

  public static Column nullable(String id, String name, ColumnType type) {
    return new Column(id, name, type, null, true);
  }

  public static Column text(String id, String name, int length) {
    return new Column(id, name, ColumnType.TEXT, length, false);
  }

  public static TableBuilder table(String id, String name) {
    return new TableBuilder(id, name);
  }

  public static Schema schema(Table... tables) {
    return new Schema(List.of(tables));
  }

  public static final class TableBuilder {

    private final String id;
    private final String name;
    private final List<Column> columns = new ArrayList<>();
    private final List<Index> indexes = new ArrayList<>();
    private final List<ForeignKey> keys = new ArrayList<>();
    private List<String> primaryKey = List.of();

    private TableBuilder(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public TableBuilder column(Column column) {
      columns.add(column);
      return this;
    }

    public TableBuilder primaryKey(String... columns) {
      primaryKey = List.of(columns);
      return this;
    }

    public TableBuilder index(boolean unique, String... columns) {
      indexes.add(Index.of(name, List.of(columns), unique));
      return this;
    }

    public TableBuilder foreignKey(String column, String table, String referenced, ForeignKey.Action action) {
      keys.add(ForeignKey.of(name, List.of(column), table, List.of(referenced), action));
      return this;
    }

    public Table build() {
      return new Table(id, name, columns, PrimaryKey.of(name, primaryKey), indexes, keys);
    }
  }
}
