package dev.horizon.leadseal;

import dev.horizon.leadseal.attribute.CollationAttribute;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.attribute.NotNullAttribute;
import dev.horizon.leadseal.change.AddColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnAttributeChange;
import dev.horizon.leadseal.change.AlterColumnTypeChange;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.change.DropColumnAttributeChange;
import dev.horizon.leadseal.change.RenameColumnChange;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A column of a table: its name, its type and its attributes.
 *
 * <p>
 * The id identifies the column across schema versions. When a column keeps its id but changes its name, a migration
 * renames the column and keeps its data. For a protobuf field, the id is the field number.
 *
 * @param id the stable identity of the column
 * @param name the column name
 * @param type the column type
 * @param attributes the attributes, at most one with each key
 */
public record Column(String id, String name, Type type, List<ColumnAttribute> attributes) {

  public Column(String id, String name, Type type, List<ColumnAttribute> attributes) {
    if (id == null || id.isBlank() || name == null || name.isBlank()) {
      throw new SchemaException("A column needs an id and a name.");
    }
    if (type == null) {
      throw new SchemaException("The column '" + name + "' has no type.");
    }
    this.id = id;
    this.name = name;
    this.type = type;
    this.attributes = attributes == null ? List.of() : List.copyOf(attributes);
    Set<Object> keys = new HashSet<>();
    for (ColumnAttribute attribute : this.attributes) {
      if (!keys.add(attribute.key())) {
        throw new SchemaException("The column '" + name + "' has two attributes with the key " + attribute.key()
            + ".");
      }
      attribute.validate(this);
    }
  }

  /**
   * Returns a column.
   *
   * @param id the stable identity of the column
   * @param name the column name
   * @param type the column type
   * @param attributes the attributes
   * @return the column
   * @throws SchemaException if an attribute does not fit the column
   */
  public static Column of(String id, String name, Type type, ColumnAttribute... attributes) {
    return new Column(id, name, type, List.of(attributes));
  }

  /**
   * Returns the attribute of a type.
   *
   * @param type the attribute type, for example {@code DefaultAttribute.class}
   * @param <A> the attribute type
   * @return the attribute, or an empty optional if the column has none
   */
  public <A extends ColumnAttribute> Optional<A> attribute(Class<A> type) {
    return attributes.stream().filter(type::isInstance).map(type::cast).findFirst();
  }

  /**
   * Returns the attributes of a type.
   *
   * @param type the attribute type, for example {@code CollationAttribute.class}
   * @param <A> the attribute type
   * @return the attributes, in order
   */
  public <A extends ColumnAttribute> List<A> attributes(Class<A> type) {
    return attributes.stream().filter(type::isInstance).map(type::cast).toList();
  }

  /**
   * Returns whether the column accepts {@code NULL}.
   *
   * @return {@code true} if the column has no {@link NotNullAttribute}
   */
  public boolean nullable() {
    return attribute(NotNullAttribute.class).isEmpty();
  }

  /**
   * Returns the collation of the column in a database.
   *
   * @param dialect the dialect name
   * @return the collation name, or an empty optional for the database default
   */
  public Optional<String> collation(String dialect) {
    return attributes(CollationAttribute.class).stream().filter(collation -> collation.dialect().equals(dialect))
        .map(CollationAttribute::name).findFirst();
  }

  /**
   * Returns a copy of this column with an attribute. It replaces the attribute with the same key.
   *
   * @param attribute the attribute
   * @return the new column
   * @throws SchemaException if the attribute does not fit the column
   */
  public Column with(ColumnAttribute attribute) {
    List<ColumnAttribute> changed = new ArrayList<>(attributes);
    changed.removeIf(existing -> existing.key().equals(attribute.key()));
    changed.add(attribute);
    return new Column(id, name, type, changed);
  }

  /**
   * Returns a copy of this column without the attributes of a type.
   *
   * @param type the attribute type
   * @return the new column
   */
  public Column without(Class<? extends ColumnAttribute> type) {
    return new Column(id, name, this.type, attributes.stream().filter(attribute -> !type.isInstance(attribute))
        .toList());
  }

  /**
   * Returns a copy of this column with another name.
   *
   * @param name the new name
   * @return the new column
   */
  public Column withName(String name) {
    return new Column(id, name, type, attributes);
  }

  /**
   * Returns the changes that turn this column into another column with the same id.
   *
   * @param table the name of the table after renames
   * @param target the column after the changes
   * @return the changes, in no particular order
   */
  List<Change> changesTo(TableName table, Column target) {
    List<Change> changes = new ArrayList<>();
    if (!name.equals(target.name)) {
      changes.add(new RenameColumnChange(table, name, target.name));
    }
    if (!type.equals(target.type)) {
      changes.add(new AlterColumnTypeChange(table, target.name, type, target.type));
    }
    Map<Object, ColumnAttribute> added = new LinkedHashMap<>();
    target.attributes.forEach(attribute -> added.put(attribute.key(), attribute));
    for (ColumnAttribute attribute : attributes) {
      ColumnAttribute now = added.remove(attribute.key());
      if (now == null) {
        changes.add(new DropColumnAttributeChange(table, target.name, attribute));
      } else if (!now.equals(attribute)) {
        changes.add(new AlterColumnAttributeChange(table, target.name, attribute, now));
      }
    }
    added.values().forEach(attribute -> changes.add(new AddColumnAttributeChange(table, target.name, attribute)));
    return changes;
  }
}
