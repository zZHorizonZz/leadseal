package dev.horizon.leadseal;

/**
 * The type of a column. Each dialect maps a type to a database type.
 *
 * <p>
 * Each type has a canonical text form for values, which default values use. Dialects turn these values into SQL
 * literals.
 */
public enum ColumnType {
  /** A boolean. Values are {@code true} and {@code false}. */
  BOOLEAN("false"),
  /** A 32-bit integer. */
  INT("0"),
  /** A 64-bit integer. */
  BIGINT("0"),
  /** A 32-bit floating-point number. */
  REAL("0"),
  /** A 64-bit floating-point number. */
  DOUBLE("0"),
  /** An exact decimal number with a precision and a scale. Values look like {@code 12.50}. */
  DECIMAL("0"),
  /** Text. A column with a length holds at most that many characters. */
  TEXT(""),
  /** Binary data. A column with a length holds at most that many bytes. Values are hexadecimal, like {@code 0aff}. */
  BYTES(""),
  /** A UUID. Values look like {@code 123e4567-e89b-12d3-a456-426614174000}. */
  UUID("00000000-0000-0000-0000-000000000000"),
  /** A point in time with microsecond precision. Values are ISO 8601 instants, like {@code 2026-10-14T07:00:00Z}. */
  TIMESTAMP("1970-01-01T00:00:00Z"),
  /** A date without a time. Values look like {@code 2026-10-14}. */
  DATE("1970-01-01"),
  /** A time of day with microsecond precision. Values look like {@code 07:00:00} or {@code 07:00:00.250}. */
  TIME("00:00:00"),
  /** A JSON document. */
  JSON("{}");

  private final String zero;

  ColumnType(String zero) {
    this.zero = zero;
  }

  /**
   * Returns the zero value of this type. Migrations fill existing rows with it when they add a column that does not
   * accept {@code NULL} and has no default.
   *
   * @return the value in its canonical text form, for example {@code 0} or {@code false}
   */
  public String zero() {
    return zero;
  }
}
