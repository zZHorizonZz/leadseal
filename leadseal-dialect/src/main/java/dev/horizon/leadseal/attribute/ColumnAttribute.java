package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;

/**
 * An attribute of a column, such as {@code NOT NULL} or a default.
 *
 * <p>
 * A column has at most one attribute with each {@linkplain #key() key}. {@link Schema#changesTo} compares the
 * attributes of a column by key: an attribute is added, dropped, or changed from one value to another.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NotNullAttribute.class, name = "not_null"),
    @JsonSubTypes.Type(value = LiteralDefaultAttribute.class, name = "default"),
    @JsonSubTypes.Type(value = NowDefaultAttribute.class, name = "default_now"),
    @JsonSubTypes.Type(value = RandomUuidDefaultAttribute.class, name = "default_random_uuid"),
    @JsonSubTypes.Type(value = IdentityAttribute.class, name = "identity"),
    @JsonSubTypes.Type(value = CollationAttribute.class, name = "collation"),
    @JsonSubTypes.Type(value = CommentAttribute.class, name = "comment")
})
public sealed interface ColumnAttribute permits NotNullAttribute, DefaultAttribute, IdentityAttribute,
    CollationAttribute, CommentAttribute {

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
}
