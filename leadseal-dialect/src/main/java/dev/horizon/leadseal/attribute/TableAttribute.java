package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Renaming;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.change.Change;

/**
 * An attribute of a table, such as its primary key, an index or a foreign key.
 *
 * <p>
 * A table has at most one attribute with each {@linkplain #key() key}. {@link Schema#changesTo} compares the attributes
 * of a table by key: an attribute is added, dropped, or changed from one value to another.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PrimaryKeyAttribute.class, name = "primary_key"),
    @JsonSubTypes.Type(value = IndexAttribute.class, name = "index"),
    @JsonSubTypes.Type(value = ForeignKeyAttribute.class, name = "foreign_key"),
    @JsonSubTypes.Type(value = CheckAttribute.class, name = "check"),
    @JsonSubTypes.Type(value = CommentAttribute.class, name = "comment")
})
public sealed interface TableAttribute permits PrimaryKeyAttribute, IndexAttribute, ForeignKeyAttribute,
    CheckAttribute, CommentAttribute {

  /**
   * Returns the key of this attribute. A table has at most one attribute with each key.
   *
   * @return the key. The default is the attribute class.
   */
  default Object key() {
    return getClass();
  }

  /**
   * Checks that this attribute fits a table.
   *
   * @param table the table that has this attribute
   * @throws SchemaException if the attribute does not fit, for example an index of a column that the table does not
   * have
   */
  void validate(Table table);

  /**
   * Returns the phase in which a migration adds this attribute, or changes another attribute into it.
   *
   * @return the phase
   */
  Change.Phase phase();

  /**
   * Returns the phase in which a migration drops this attribute.
   *
   * @return the phase. The default is {@link #phase()}.
   */
  default Change.Phase dropPhase() {
    return phase();
  }

  /**
   * Returns whether a migration can change this attribute into another one with the same key. A migration drops an
   * attribute that it cannot change and adds the new one.
   *
   * @return {@code true} if the attribute can change. The default is {@code false}.
   */
  default boolean alterable() {
    return false;
  }

  /**
   * Returns a short description of this attribute, which names a migration that changes it.
   *
   * @param table the name of the table that has this attribute
   * @return the description, for example {@code ix_orders_state}
   */
  String summary(String table);

  /**
   * Returns whether a change of this attribute only supports other changes, such as a new index. A migration is
   * named after its changes that are not minor.
   *
   * @return {@code true} if the change is minor. The default is {@code true}.
   */
  default boolean minor() {
    return true;
  }

  /**
   * Returns this attribute with the names that tables and columns have after renames. A migration compares the
   * renamed attribute of the old table with the attribute of the new table.
   *
   * @param table the name of the table that has this attribute, before the renames
   * @param renaming the renames
   * @return the renamed attribute. The default is this attribute.
   */
  default TableAttribute renamed(TableName table, Renaming renaming) {
    return this;
  }
}
