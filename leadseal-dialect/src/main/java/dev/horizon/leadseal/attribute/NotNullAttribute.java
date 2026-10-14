package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;

/** A column that does not accept {@code NULL}. */
public record NotNullAttribute() implements ColumnAttribute {

  public static final NotNullAttribute INSTANCE = new NotNullAttribute();

  @Override
  public void validate(Column column) {
  }
}
