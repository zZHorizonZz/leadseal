package dev.horizon.leadseal;

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
 * @param nullable {@code true} if the column accepts {@code NULL}
 */
public record Column(String id, String name, ColumnType type, Integer length, boolean nullable) {

  public Column {
    Names.require(id, "column id");
    Names.require(name, "column name");
    if (type == null) {
      throw new SchemaException("The column '" + name + "' has no type.");
    }
    if (length != null && length <= 0) {
      throw new SchemaException("The length of the column '" + name + "' must be greater than 0, but is " + length
          + ".");
    }
    if (length != null && type != ColumnType.TEXT && type != ColumnType.BYTES) {
      throw new SchemaException("The column '" + name + "' has a length, but only TEXT and BYTES columns can have"
          + " one.");
    }
  }

  /**
   * Returns a copy of this column with another name.
   *
   * @param name the new name
   * @return the new column
   */
  public Column withName(String name) {
    return new Column(id, name, type, length, nullable);
  }
}
