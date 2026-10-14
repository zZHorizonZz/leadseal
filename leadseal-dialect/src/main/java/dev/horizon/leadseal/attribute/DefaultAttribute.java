package dev.horizon.leadseal.attribute;

/**
 * The default value of a column: a literal, or a value that the database generates for each new row. A column has at
 * most one default.
 */
public sealed interface DefaultAttribute extends ColumnAttribute permits LiteralDefaultAttribute,
    NowDefaultAttribute, RandomUuidDefaultAttribute {

  @Override
  default Object key() {
    return DefaultAttribute.class;
  }
}
