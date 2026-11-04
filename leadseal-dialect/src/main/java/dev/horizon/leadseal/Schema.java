package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.ForeignKey;

import dev.horizon.leadseal.change.Change;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The tables of a database, in their namespaces. The constructor checks that namespaces are unique, that table ids are
 * unique across namespaces, and that every foreign key references the primary key or a unique index of a table of this
 * schema.
 *
 * @param namespaces the namespaces
 */
public record Schema(List<Namespace> namespaces) {

  public Schema(List<Namespace> namespaces) {
    this.namespaces = List.copyOf(namespaces);
    Set<Namespace.Name> names = new HashSet<>();
    Set<String> ids = new HashSet<>();
    for (Namespace namespace : this.namespaces) {
      if (!names.add(namespace.name())) {
        throw new InvalidSchemaException("The schema has the namespace " + namespace.name() + " twice.");
      }
      for (Table table : namespace.tables()) {
        if (!ids.add(table.id())) {
          throw new InvalidSchemaException("The schema has the table id '" + table.id() + "' twice.");
        }
      }
    }
    for (TableName name : names()) {
      Table table = table(name).orElseThrow();
      for (ForeignKey key : table.attributes(ForeignKey.class)) {
        key.validate(table, this);
      }
    }
  }

  /**
   * Returns a schema with tables in the default namespace.
   *
   * @param tables the tables
   * @return the schema
   */
  public static Schema of(Table... tables) {
    return new Schema(tables.length == 0 ? List.of() : List.of(Namespace.defaults(tables)));
  }

  /**
   * Returns a schema with namespaces.
   *
   * @param namespaces the namespaces
   * @return the schema
   */
  public static Schema of(Namespace... namespaces) {
    return new Schema(List.of(namespaces));
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
   * Returns the names of all tables, namespace by namespace.
   *
   * @return the table names
   */
  public List<TableName> names() {
    return namespaces.stream().flatMap(namespace -> namespace.tables().stream().map(namespace::nameOf)).toList();
  }

  /**
   * Returns a table.
   *
   * @param name the table name
   * @return the table, or an empty optional if the schema has no such table
   */
  public Optional<Table> table(TableName name) {
    return namespaces.stream().filter(namespace -> namespace.name().equals(name.namespace())).findFirst()
        .flatMap(namespace -> namespace.table(name.name()));
  }

  /**
   * Returns the name of the table with an id.
   *
   * @param id the table id
   * @return the name, or an empty optional if the schema has no table with that id
   */
  public Optional<TableName> nameOf(String id) {
    return names().stream().filter(name -> table(name).orElseThrow().id().equals(id)).findFirst();
  }

  /**
   * Returns the table with an id.
   *
   * @param id the table id
   * @return the table, or an empty optional if the schema has no table with that id
   */
  public Optional<Table> tableById(String id) {
    return nameOf(id).flatMap(this::table);
  }

  /**
   * Returns the changes that turn this schema into another one, in the order they run.
   *
   * <p>
   * Tables and columns are matched by id. A table or column that keeps its id but changes its name is renamed, and
   * keeps its data. Attributes are matched by key: an attribute is added, dropped, or changed to another attribute with
   * the same key. A changed index, check or foreign key is dropped and added again. The changes run phase by phase, in
   * the order of {@link Change.Phase}.
   *
   * @param target the schema after the changes
   * @return the changes, or an empty list if the schemas have the same tables
   */
  public List<Change> changesTo(Schema target) {
    Transition transition = new Transition(this, target);
    List<Change> changes = new ArrayList<>();
    for (TableName name : names()) {
      Table table = table(name).orElseThrow();
      Optional<TableName> targetName = target.nameOf(table.id());
      changes.addAll(targetName.isEmpty() ? table.dropChanges(name)
          : table.changesTo(name, targetName.get(), target.table(targetName.get()).orElseThrow(), transition));
    }
    for (TableName name : target.names()) {
      Table table = target.table(name).orElseThrow();
      if (tableById(table.id()).isEmpty()) {
        changes.addAll(table.createChanges(name));
      }
    }
    changes.sort(Comparator.comparing(Change::phase));
    return List.copyOf(changes);
  }
}
