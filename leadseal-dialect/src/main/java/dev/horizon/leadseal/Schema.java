package dev.horizon.leadseal;

import dev.horizon.leadseal.impl.Json;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A set of tables. The constructor checks that names are unique and that every foreign key references the primary key
 * or a unique index of an existing table, with columns of the same types.
 *
 * @param tables the tables
 */
public record Schema(List<Table> tables) {

  public Schema {
    tables = List.copyOf(tables);
    Set<String> names = new HashSet<>();
    Set<String> ids = new HashSet<>();
    for (Table table : tables) {
      if (!names.add(table.name())) {
        throw new SchemaException("The schema has the table name '" + table.name() + "' twice.");
      }
      if (!ids.add(table.id())) {
        throw new SchemaException("The schema has the table id '" + table.id() + "' twice.");
      }
    }
    for (Table table : tables) {
      for (ForeignKey key : table.foreignKeys()) {
        check(table, key, tables);
      }
    }
  }

  /**
   * Returns a schema without tables.
   *
   * @return the empty schema
   */
  public static Schema empty() {
    return new Schema(List.of());
  }

  /**
   * Returns the table with a name.
   *
   * @param name the table name
   * @return the table, or an empty optional if the schema has no table with that name
   */
  public Optional<Table> table(String name) {
    return tables.stream().filter(table -> table.name().equals(name)).findFirst();
  }

  /**
   * Returns the table with an id.
   *
   * @param id the table id
   * @return the table, or an empty optional if the schema has no table with that id
   */
  public Optional<Table> tableById(String id) {
    return tables.stream().filter(table -> table.id().equals(id)).findFirst();
  }

  /**
   * Returns a hash of this schema. Two schemas with the same tables, in the same order, have the same hash.
   *
   * @return the first 16 hexadecimal digits of the SHA-256 hash of the schema's JSON form
   */
  public String hash() {
    return Names.hash(Json.write(this, false)).substring(0, 16);
  }

  private static void check(Table table, ForeignKey key, List<Table> tables) {
    Table target = tables.stream().filter(candidate -> candidate.name().equals(key.table())).findFirst()
        .orElseThrow(() -> new SchemaException("The foreign key '" + key.name() + "' references the table '"
            + key.table() + "', which the schema does not have."));
    List<String> referenced = key.referencedColumns();
    boolean unique = target.primaryKey().columns().equals(referenced)
        || target.indexes().stream().anyMatch(index -> index.unique() && index.columns().equals(referenced));
    if (!unique) {
      throw new SchemaException("The foreign key '" + key.name() + "' references " + target.name() + referenced
          + ", which is neither the primary key nor a unique index of that table.");
    }
    for (int i = 0; i < referenced.size(); i++) {
      Column column = table.column(key.columns().get(i)).orElseThrow();
      Column targetColumn = target.column(referenced.get(i)).orElseThrow(() -> new SchemaException("The foreign key '"
          + key.name() + "' references a column that the table '" + target.name() + "' does not have."));
      if (column.type() != targetColumn.type()) {
        throw new SchemaException("The foreign key '" + key.name() + "' joins " + table.name() + "." + column.name()
            + " (" + column.type() + ") with " + target.name() + "." + targetColumn.name() + " (" + targetColumn.type()
            + "). The types must be the same.");
      }
    }
  }
}
