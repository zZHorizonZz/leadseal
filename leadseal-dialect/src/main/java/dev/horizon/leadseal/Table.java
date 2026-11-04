package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.change.AddColumn;
import dev.horizon.leadseal.change.AddTableAttribute;
import dev.horizon.leadseal.change.AlterTableAttribute;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateTable;
import dev.horizon.leadseal.change.DropColumn;
import dev.horizon.leadseal.change.DropTable;
import dev.horizon.leadseal.change.DropTableAttribute;
import dev.horizon.leadseal.change.MoveTable;
import dev.horizon.leadseal.change.RenameTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A table: its name, its columns and its attributes, such as the primary key and the indexes.
 *
 * <p>
 * The id identifies the table across schema versions. When a table keeps its id but changes its name, a migration
 * renames the table and keeps its rows. For a protobuf message, the id is the full message name without the version
 * parts of its package.
 *
 * @param id the stable identity of the table
 * @param name the table name, unique in its namespace
 * @param columns the columns, in order
 * @param attributes the attributes, at most one with each key. Every table has a {@link PrimaryKey}.
 */
public record Table(String id, String name, List<Column> columns, List<TableAttribute> attributes) {

  public Table(String id, String name, List<Column> columns, List<TableAttribute> attributes) {
    if (id == null || id.isBlank() || name == null || name.isBlank()) {
      throw new InvalidSchemaException("A table needs an id and a name.");
    }
    this.id = id;
    this.name = name;
    this.columns = List.copyOf(columns);
    this.attributes = attributes == null ? List.of() : List.copyOf(attributes);
    if (this.columns.isEmpty()) {
      throw new InvalidSchemaException("The table '" + name + "' has no columns.");
    }
    unique(this.columns.stream().map(Column::name).toList(), "column name");
    unique(this.columns.stream().map(Column::id).toList(), "column id");
    unique(this.attributes.stream().map(TableAttribute::key).toList(), "attribute");
    primaryKey();
    this.attributes.forEach(attribute -> attribute.validate(this));
    this.columns.forEach(column -> column.attributes().forEach(attribute -> attribute.validate(this, column)));
  }

  /**
   * Returns a table.
   *
   * @param id the stable identity of the table
   * @param name the table name
   * @param columns the columns
   * @param attributes the attributes, with a {@link PrimaryKey}
   * @return the table
   * @throws SchemaException if the table has no primary key, or an attribute does not fit it
   */
  public static Table of(String id, String name, List<Column> columns, TableAttribute... attributes) {
    return new Table(id, name, columns, List.of(attributes));
  }

  /**
   * Returns the column with a name.
   *
   * @param name the column name
   * @return the column, or an empty optional if the table has no column with that name
   */
  public Optional<Column> column(String name) {
    return columns.stream().filter(column -> column.name().equals(name)).findFirst();
  }

  /**
   * Returns the column with an id.
   *
   * @param id the column id
   * @return the column, or an empty optional if the table has no column with that id
   */
  public Optional<Column> columnById(String id) {
    return columns.stream().filter(column -> column.id().equals(id)).findFirst();
  }

  /**
   * Returns the attribute of a type.
   *
   * @param type the attribute type, for example {@code Comment.class}
   * @param <A> the attribute type
   * @return the attribute, or an empty optional if the table has none
   */
  public <A extends TableAttribute> Optional<A> attribute(Class<A> type) {
    return attributes.stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  /**
   * Returns the attributes of a type.
   *
   * @param type the attribute type, for example {@code Index.class}
   * @param <A> the attribute type
   * @return the attributes, in order
   */
  public <A extends TableAttribute> List<A> attributes(Class<A> type) {
    return attributes.stream().filter(type::isInstance).map(type::cast).toList();
  }

  /**
   * Returns the primary key.
   *
   * @return the primary key
   */
  public PrimaryKey primaryKey() {
    return attribute(PrimaryKey.class).orElseThrow(() -> new InvalidSchemaException("The table '" + name
        + "' has no primary key."));
  }

  /**
   * Tells if a column is part of the primary key, an index or a foreign key.
   *
   * @param column the column name
   * @return {@code true} if a key or an index contains the column
   */
  public boolean covers(String column) {
    return attributes.stream().anyMatch(attribute -> attribute.covers(column));
  }

  /**
   * Returns a copy of this table with an attribute. It replaces the attribute with the same key.
   *
   * @param attribute the attribute
   * @return the new table
   * @throws SchemaException if the attribute does not fit the table
   */
  public Table with(TableAttribute attribute) {
    List<TableAttribute> changed = new ArrayList<>(attributes);
    changed.removeIf(existing -> existing.key().equals(attribute.key()));
    changed.add(attribute);
    return new Table(id, name, columns, changed);
  }

  /**
   * Returns a copy of this table without the attributes of a type.
   *
   * @param type the attribute type
   * @return the new table
   */
  public Table without(Class<? extends TableAttribute> type) {
    return new Table(id, name, columns, attributes.stream().filter(attribute -> !type.isInstance(attribute))
        .toList());
  }

  /**
   * Returns a copy of this table with other columns.
   *
   * @param columns the columns
   * @return the new table
   */
  public Table withColumns(List<Column> columns) {
    return new Table(id, name, columns, attributes);
  }

  /**
   * Returns the changes that create this table: the table without its foreign keys, and then its foreign keys.
   *
   * @param name the table name
   * @return the changes
   */
  List<Change> createChanges(TableName name) {
    List<Change> changes = new ArrayList<>();
    changes.add(new CreateTable(name.namespace(), without(ForeignKey.class)));
    attributes(ForeignKey.class).forEach(key -> changes.add(new AddTableAttribute(name, key)));
    return changes;
  }

  /**
   * Returns the changes that drop this table: its foreign keys, and then the table.
   *
   * @param name the table name
   * @return the changes
   */
  List<Change> dropChanges(TableName name) {
    List<Change> changes = new ArrayList<>();
    attributes(ForeignKey.class).forEach(key -> changes.add(new DropTableAttribute(name, key)));
    changes.add(new DropTable(name));
    return changes;
  }

  /**
   * Returns the changes that turn this table into another table with the same id. Columns are matched by id and
   * attributes by key. An attribute that cannot change is dropped and added again.
   *
   * @param name the name of this table
   * @param targetName the name of the table after the changes
   * @param target the table after the changes
   * @param transition the origin and target schemas
   * @return the changes, in no particular order
   */
  List<Change> changesTo(TableName name, TableName targetName, Table target, Transition transition) {
    List<Change> changes = new ArrayList<>(nameChanges(name, targetName));
    changes.addAll(columnChanges(targetName, target));
    changes.addAll(attributeChanges(name, targetName, target, transition));
    return changes;
  }

  private List<Change> nameChanges(TableName name, TableName targetName) {
    List<Change> changes = new ArrayList<>();
    if (!name.name().equals(targetName.name())) {
      changes.add(new RenameTable(name, targetName.name()));
    }
    if (!name.namespace().equals(targetName.namespace())) {
      changes.add(new MoveTable(name.withName(targetName.name()), targetName.namespace()));
    }
    return changes;
  }

  private List<Change> columnChanges(TableName targetName, Table target) {
    List<Change> changes = new ArrayList<>();
    for (Column column : columns) {
      target.columnById(column.id()).ifPresentOrElse(other -> changes.addAll(column.changesTo(targetName, other)),
          () -> changes.add(new DropColumn(targetName, column.name())));
    }
    target.columns.stream().filter(column -> columnById(column.id()).isEmpty())
        .forEach(column -> changes.add(new AddColumn(targetName, column)));
    return changes;
  }

  private List<Change> attributeChanges(TableName name, TableName targetName, Table target, Transition transition) {
    List<Change> changes = new ArrayList<>();
    Map<Object, TableAttribute> added = new LinkedHashMap<>();
    target.attributes.forEach(attribute -> added.put(attribute.key(), attribute));
    for (TableAttribute attribute : attributes) {
      TableAttribute origin = attribute.rename(name, transition);
      TableAttribute other = added.remove(origin.key());
      if (other == null) {
        changes.add(dropChange(name, targetName, attribute));
      } else if (!other.equals(origin) && other.alterable()) {
        changes.add(new AlterTableAttribute(targetName, origin, other));
      } else if (!other.equals(origin)) {
        changes.add(dropChange(name, targetName, attribute));
        changes.add(new AddTableAttribute(targetName, other));
      }
    }
    added.values().forEach(attribute -> changes.add(new AddTableAttribute(targetName, attribute)));
    return changes;
  }

  private Change dropChange(TableName name, TableName targetName, TableAttribute attribute) {
    boolean beforeRenames = attribute.dropPhase().precedes(Change.Phase.RENAME_TABLES);
    return new DropTableAttribute(beforeRenames ? name : targetName, attribute);
  }

  private void unique(List<?> values, String what) {
    Set<Object> distinct = new HashSet<>();
    for (Object value : values) {
      if (!distinct.add(value)) {
        throw new InvalidSchemaException("The table '" + name + "' has the " + what + " " + value + " twice.");
      }
    }
  }
}
