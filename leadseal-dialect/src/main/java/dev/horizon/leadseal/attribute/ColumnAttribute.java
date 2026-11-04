package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * An attribute of a column, such as {@code NOT NULL} or a default.
 *
 * <p>
 * A column has at most one attribute with each {@linkplain #key() key}. {@link Schema#changesTo} compares the
 * attributes of a column by key: an attribute is added, dropped, or changed from one value to another.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NotNull.class, name = "not_null"),
    @JsonSubTypes.Type(value = LiteralDefault.class, name = "default"),
    @JsonSubTypes.Type(value = NowDefault.class, name = "default_now"),
    @JsonSubTypes.Type(value = RandomUuidDefault.class, name = "default_random_uuid"),
    @JsonSubTypes.Type(value = Identity.class, name = "identity"),
    @JsonSubTypes.Type(value = Collation.class, name = "collation"),
    @JsonSubTypes.Type(value = Comment.class, name = "comment")
})
public sealed interface ColumnAttribute permits NotNull, Default, Identity,
    Collation, Comment {

  /**
   * Returns the key of this attribute. A column has at most one attribute with each key.
   *
   * @return the key. The default is the attribute class.
   */
  default Object key() {
    return getClass();
  }

  /**
   * Checks that this attribute fits a column.
   *
   * @param column the column that has this attribute
   * @throws SchemaException if the attribute does not fit, for example a collation on a number column
   */
  void validate(Column column);

  /**
   * Checks that this attribute fits a column in its table.
   *
   * @param table the table of the column
   * @param column the column that has this attribute
   * @throws SchemaException if the attribute does not fit the table. The default accepts every table.
   */
  default void validate(Table table, Column column) {
  }

  /**
   * Gives the commands that add this attribute to a column that has rows already.
   *
   * @param ddl the statements of the dialect
   * @param column the column, which has this attribute after the migration
   * @return the commands
   */
  List<Command> add(Ddl ddl, ColumnTransition column);

  /**
   * Gives the commands that drop this attribute from a column.
   *
   * @param ddl the statements of the dialect
   * @param column the column, which has this attribute before the migration
   * @return the commands
   */
  List<Command> drop(Ddl ddl, ColumnTransition column);

  /**
   * Gives the commands that change an attribute with the key of this attribute into this attribute.
   *
   * @param ddl the statements of the dialect
   * @param column the column, which has this attribute after the migration
   * @return the commands. The default is the commands that add this attribute.
   */
  default List<Command> alter(Ddl ddl, ColumnTransition column) {
    return add(ddl, column);
  }

}
