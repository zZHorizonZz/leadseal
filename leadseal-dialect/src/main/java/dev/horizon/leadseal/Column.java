package dev.horizon.leadseal;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A column of a table.
 *
 * <p>
 * The id identifies the column across schema versions. When a column keeps its id but changes its name, the diff
 * renames the column and keeps its data. For a protobuf field, the id is the field number.
 *
 * @param id the stable identity of the column
 * @param name the column name
 * @param type the column type
 * @param length the maximum length of a {@code TEXT} or {@code BYTES} column, or {@code null} for no limit
 * @param precision the number of digits of a {@code DECIMAL} column
 * @param scale the number of digits after the decimal point of a {@code DECIMAL} column, or {@code null} for 0
 * @param nullable {@code true} if the column accepts {@code NULL}
 * @param defaultValue the default value, or {@code null} for none
 * @param identity {@code true} if the database generates the values, for an {@code INT} or {@code BIGINT} primary key
 * @param collations the collation of a {@code TEXT} column for each dialect, by dialect name. A dialect without an
 * entry uses the database default.
 * @param comment the comment, or {@code null} for none
 */
public record Column(String id, String name, ColumnType type, Integer length, Integer precision, Integer scale,
    boolean nullable, Default defaultValue, boolean identity, Map<String, String> collations, String comment) {

  private static final Set<ColumnType> SIZED = Set.of(ColumnType.TEXT, ColumnType.BYTES);
  private static final Set<ColumnType> COUNTED = Set.of(ColumnType.INT, ColumnType.BIGINT);
  private static final Set<ColumnType> DATED = Set.of(ColumnType.TIMESTAMP, ColumnType.DATE);

  public Column {
    Names.require(id, "column id");
    Names.require(name, "column name");
    if (type == null) {
      throw new SchemaException("The column '" + name + "' has no type.");
    }
    collations = collations == null ? Map.of() : Map.copyOf(new TreeMap<>(collations));
    comment = comment == null || comment.isBlank() ? null : comment;
    positive(name, "length", length);
    positive(name, "precision", precision);
    if (length != null && !SIZED.contains(type)) {
      throw new SchemaException("The column '" + name + "' has a length, but only TEXT and BYTES columns can have"
          + " one.");
    }
    if ((precision != null || scale != null) && type != ColumnType.DECIMAL) {
      throw new SchemaException("The column '" + name + "' has a precision or a scale, but only DECIMAL columns can"
          + " have them.");
    }
    if (type == ColumnType.DECIMAL && precision == null) {
      throw new SchemaException("The DECIMAL column '" + name + "' needs a precision.");
    }
    if (scale != null && (scale < 0 || scale > precision)) {
      throw new SchemaException("The scale of the column '" + name + "' must be between 0 and its precision "
          + precision + ", but is " + scale + ".");
    }
    if (!collations.isEmpty() && type != ColumnType.TEXT) {
      throw new SchemaException("The column '" + name + "' has a collation, but only TEXT columns can have one.");
    }
    if (identity && (!COUNTED.contains(type) || nullable || defaultValue != null)) {
      throw new SchemaException("The identity column '" + name + "' must be an INT or BIGINT column that does not"
          + " accept NULL and has no default.");
    }
    if (defaultValue != null) {
      checkDefault(name, type, defaultValue);
    }
  }

  public Column(String id, String name, ColumnType type, Integer length, boolean nullable) {
    this(id, name, type, length, null, null, nullable, null, false, Map.of(), null);
  }

  /**
   * Returns a copy of this column with another name.
   *
   * @param name the new name
   * @return the new column
   */
  public Column withName(String name) {
    return new Column(id, name, type, length, precision, scale, nullable, defaultValue, identity, collations,
        comment);
  }

  /**
   * Returns a copy of this column with a precision and a scale, for a {@code DECIMAL} column.
   *
   * @param precision the number of digits
   * @param scale the number of digits after the decimal point
   * @return the new column
   */
  public Column withPrecision(int precision, int scale) {
    return new Column(id, name, type, length, precision, scale, nullable, defaultValue, identity, collations,
        comment);
  }

  /**
   * Returns a copy of this column with another default.
   *
   * @param defaultValue the default, or {@code null} for none
   * @return the new column
   */
  public Column withDefault(Default defaultValue) {
    return new Column(id, name, type, length, precision, scale, nullable, defaultValue, identity, collations,
        comment);
  }

  /**
   * Returns a copy of this column whose values the database generates, or does not generate.
   *
   * @param identity {@code true} for an identity column
   * @return the new column
   */
  public Column withIdentity(boolean identity) {
    return new Column(id, name, type, length, precision, scale, nullable, defaultValue, identity, collations,
        comment);
  }

  /**
   * Returns a copy of this column with other collations.
   *
   * @param collations the collation for each dialect, by dialect name
   * @return the new column
   */
  public Column withCollations(Map<String, String> collations) {
    return new Column(id, name, type, length, precision, scale, nullable, defaultValue, identity, collations,
        comment);
  }

  /**
   * Returns a copy of this column with another comment.
   *
   * @param comment the comment, or {@code null} for none
   * @return the new column
   */
  public Column withComment(String comment) {
    return new Column(id, name, type, length, precision, scale, nullable, defaultValue, identity, collations,
        comment);
  }

  private static void positive(String name, String what, Integer value) {
    if (value != null && value <= 0) {
      throw new SchemaException("The " + what + " of the column '" + name + "' must be greater than 0, but is "
          + value + ".");
    }
  }

  private static void checkDefault(String name, ColumnType type, Default value) {
    switch (value.kind()) {
      case NOW -> {
        if (!DATED.contains(type)) {
          throw new SchemaException("The column '" + name + "' defaults to the current time, but only TIMESTAMP and"
              + " DATE columns can.");
        }
      }
      case RANDOM_UUID -> {
        if (type != ColumnType.UUID) {
          throw new SchemaException("The column '" + name + "' defaults to a random UUID, but only UUID columns"
              + " can.");
        }
      }
      case VALUE -> {
        if (!Values.valid(type, value.value())) {
          throw new SchemaException("The default '" + value.value() + "' of the column '" + name + "' is not a valid "
              + type + " value." + Values.example(type));
        }
      }
    }
  }
}
