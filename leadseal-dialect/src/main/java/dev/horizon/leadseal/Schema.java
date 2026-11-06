package dev.horizon.leadseal;

import dev.horizon.leadseal.change.AlterEnumType;

import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateEnumType;
import dev.horizon.leadseal.change.DropEnumType;
import dev.horizon.leadseal.type.EnumType;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The tables of a database, in their namespaces. The constructor checks that namespaces are unique, that table ids are
 * unique across namespaces, and that every foreign key references the primary key or a unique index of a table of this
 * schema.
 *
 * @param namespaces the namespaces
 */
public record Schema(List<Namespace> namespaces) {

  public Schema(List<Namespace> namespaces) {
    this.namespaces = namespaces == null ? List.of() : List.copyOf(namespaces);
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
      table.attributes().forEach(attribute -> attribute.validate(table, this));
    }
    Map<String, List<EnumType.Value>> types = new LinkedHashMap<>();
    for (EnumType type : columnTypes(EnumType.class).toList()) {
      List<EnumType.Value> values = types.putIfAbsent(type.name(), type.values());
      if (values != null && !values.equals(type.values())) {
        throw new InvalidSchemaException("The schema has two enums named " + type.name() + " with other values. Give"
            + " one of them another name.");
      }
      if (names().stream().anyMatch(name -> name.name().equals(type.name()))) {
        throw new InvalidSchemaException("The schema has the enum " + type.name() + " and a table with the same name."
            + " Give one of them another name.");
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
   * Returns the enum types of the columns that store value names. A dialect with enum types, such as PostgreSQL,
   * creates a type of each of them, which the columns share.
   *
   * @return the enums, each name once
   */
  public List<EnumType> enumTypes() {
    return columnTypes(EnumType.class).filter(type -> type.storage().select(true, false)).distinct().toList();
  }

  private <T extends Type> Stream<T> columnTypes(Class<T> kind) {
    return names().stream().flatMap(name -> table(name).orElseThrow().columns().stream()).map(Column::type)
        .filter(kind::isInstance).map(kind::cast);
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
    Map<String, EnumType> origins = new LinkedHashMap<>();
    enumTypes().forEach(type -> origins.put(type.name(), type));
    for (EnumType type : target.enumTypes()) {
      EnumType origin = origins.remove(type.name());
      if (origin == null) {
        changes.add(new CreateEnumType(type));
      } else if (!origin.equals(type)) {
        changes.add(new AlterEnumType(origin, type));
      }
    }
    origins.values().forEach(type -> changes.add(new DropEnumType(type)));
    changes.sort(Comparator.comparing(Change::phase));
    return List.copyOf(changes);
  }
}
