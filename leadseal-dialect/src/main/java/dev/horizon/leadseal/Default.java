package dev.horizon.leadseal;

import java.util.List;

/**
 * The default value of a column: a fixed value, or a value that the database generates for each row.
 *
 * @param kind the kind of default
 * @param value the value in the canonical text form of the column type, for a {@link Kind#VALUE} default
 */
public record Default(Kind kind, String value) {

  public Default {
    if (kind == null) {
      throw new SchemaException("A default has no kind.");
    }
    if ((kind == Kind.VALUE) != (value != null)) {
      throw new SchemaException("A " + kind + " default " + (kind == Kind.VALUE ? "needs" : "cannot have")
          + " a value.");
    }
  }

  /** The kind of a default. */
  public enum Kind {
    /** A fixed value. */
    VALUE,
    /** The current time, or the current date for a {@code DATE} column, in UTC. */
    NOW,
    /** A random UUID. */
    RANDOM_UUID
  }

  /**
   * Returns a fixed default.
   *
   * @param value the value in the canonical text form of the column type, for example {@code 0} or {@code new}
   * @return the default
   */
  public static Default value(String value) {
    return new Default(Kind.VALUE, value);
  }

  /**
   * Returns a default of the current time, or the current date for a {@code DATE} column.
   *
   * @return the default
   */
  public static Default now() {
    return new Default(Kind.NOW, null);
  }

  /**
   * Returns a default of a random UUID.
   *
   * @return the default
   */
  public static Default randomUuid() {
    return new Default(Kind.RANDOM_UUID, null);
  }

  /**
   * Returns the name of the constraint that holds a column default, for databases that store defaults as
   * constraints.
   *
   * @param table the table name
   * @param column the column name
   * @return the name, {@code df_} followed by the table name and the column name
   */
  public static String constraintName(String table, String column) {
    return Names.generate("df", table, List.of(column));
  }
}
