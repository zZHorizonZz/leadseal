package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IdentityAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.attribute.TableAttribute;
import dev.horizon.leadseal.change.AddColumnChange;
import dev.horizon.leadseal.change.AddTableAttributeChange;
import dev.horizon.leadseal.change.AlterTableAttributeChange;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.CreateTableChange;
import dev.horizon.leadseal.change.DropColumnChange;
import dev.horizon.leadseal.change.DropTableAttributeChange;
import dev.horizon.leadseal.change.DropTableChange;
import dev.horizon.leadseal.change.MoveTableChange;
import dev.horizon.leadseal.change.RenameTableChange;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * @param attributes the attributes, at most one with each key. Every table has a {@link PrimaryKeyAttribute}.
 */
public record Table(String id, String name, List<Column> columns, List<TableAttribute> attributes) {

  public Table(String id, String name, List<Column> columns, List<TableAttribute> attributes) {
    if (id == null || id.isBlank() || name == null || name.isBlank()) {
      throw new SchemaException("A table needs an id and a name.");
    }
    this.id = id;
    this.name = name;
    this.columns = List.copyOf(columns);
    this.attributes = attributes == null ? List.of() : List.copyOf(attributes);
    if (this.columns.isEmpty()) {
      throw new SchemaException("The table '" + name + "' has no columns.");
    }
    unique(name, this.columns.stream().map(Column::name).toList(), "column name");
    unique(name, this.columns.stream().map(Column::id).toList(), "column id");
    unique(name, this.attributes.stream().map(TableAttribute::key).toList(), "attribute");
    primaryKey();
    for (TableAttribute attribute : this.attributes) {
      attribute.validate(this);
    }
    for (Column column : this.columns) {
      if (column.attribute(IdentityAttribute.class).isPresent()
          && !primaryKey().columns().equals(List.of(column.name()))) {
        throw new SchemaException("The identity column '" + name + "." + column.name() + "' must be the only column"
            + " of the primary key.");
      }
    }
  }

  /**
   * Returns a table.
   *
   * @param id the stable identity of the table
   * @param name the table name
   * @param columns the columns
   * @param attributes the attributes, with a {@link PrimaryKeyAttribute}
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
   * @param type the attribute type, for example {@code CommentAttribute.class}
   * @param <A> the attribute type
   * @return the attribute, or an empty optional if the table has none
   */
  public <A extends TableAttribute> Optional<A> attribute(Class<A> type) {
    return attributes.stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  /**
   * Returns the attributes of a type.
   *
   * @param type the attribute type, for example {@code IndexAttribute.class}
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
  public PrimaryKeyAttribute primaryKey() {
    return attribute(PrimaryKeyAttribute.class).orElseThrow(() -> new SchemaException("The table '" + name
        + "' has no primary key."));
  }

  /**
   * Returns whether a column is part of the primary key, an index or a foreign key.
   *
   * @param column the column name
   * @return {@code true} if a key or an index contains the column
   */
  public boolean keyed(String column) {
    return primaryKey().columns().contains(column)
        || attributes(IndexAttribute.class).stream().anyMatch(index -> index.contains(column))
        || attributes(ForeignKeyAttribute.class).stream().anyMatch(key -> key.columns().contains(column));
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
    changes.add(new CreateTableChange(name.namespace(), without(ForeignKeyAttribute.class)));
    attributes(ForeignKeyAttribute.class).forEach(key -> changes.add(new AddTableAttributeChange(name, key)));
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
    attributes(ForeignKeyAttribute.class).forEach(key -> changes.add(new DropTableAttributeChange(name, key)));
    changes.add(new DropTableChange(name));
    return changes;
  }

  /**
   * Returns the changes that turn this table into another table with the same id. Columns are matched by id and
   * attributes by key. An attribute that cannot change is dropped and added again.
   *
   * @param name the name of this table
   * @param targetName the name of the table after the changes
   * @param target the table after the changes
   * @param renaming the new names of tables and columns
   * @return the changes, in no particular order
   */
  List<Change> changesTo(TableName name, TableName targetName, Table target, Renaming renaming) {
    List<Change> changes = new ArrayList<>();
    if (!name.name().equals(targetName.name())) {
      changes.add(new RenameTableChange(name, targetName.name()));
    }
    if (!Objects.equals(name.namespace(), targetName.namespace())) {
      changes.add(new MoveTableChange(name.withName(targetName.name()), targetName.namespace()));
    }
    for (Column column : columns) {
      Optional<Column> now = target.columnById(column.id());
      if (now.isEmpty()) {
        changes.add(new DropColumnChange(targetName, column.name()));
      } else {
        changes.addAll(column.changesTo(targetName, now.get()));
      }
    }
    for (Column column : target.columns) {
      if (columnById(column.id()).isEmpty()) {
        changes.add(new AddColumnChange(targetName, column));
      }
    }
    Map<Object, TableAttribute> added = new LinkedHashMap<>();
    target.attributes.forEach(attribute -> added.put(attribute.key(), attribute));
    for (TableAttribute attribute : attributes) {
      TableAttribute renamed = attribute.renamed(name, renaming);
      TableAttribute now = added.remove(renamed.key());
      if (now == null) {
        changes.add(dropChange(name, targetName, attribute));
      } else if (now.equals(renamed)) {
        continue;
      } else if (now.alterable()) {
        changes.add(new AlterTableAttributeChange(targetName, renamed, now));
      } else {
        changes.add(dropChange(name, targetName, attribute));
        changes.add(new AddTableAttributeChange(targetName, now));
      }
    }
    added.values().forEach(attribute -> changes.add(new AddTableAttributeChange(targetName, attribute)));
    return changes;
  }

  /** Returns the change that drops an attribute. A drop that runs before tables are renamed uses the old name. */
  private Change dropChange(TableName name, TableName targetName, TableAttribute attribute) {
    boolean beforeRenames = attribute.dropPhase().compareTo(Change.Phase.RENAME_TABLES) < 0;
    return new DropTableAttributeChange(beforeRenames ? name : targetName, attribute);
  }

  private static void unique(String table, List<?> values, String what) {
    Set<Object> seen = new HashSet<>();
    for (Object value : values) {
      if (!seen.add(value)) {
        throw new SchemaException("The table '" + table + "' has the " + what + " " + value + " twice.");
      }
    }
  }
}
