package dev.horizon.leadseal;

/** The type of a column. Each dialect maps a type to a database type. */
public enum ColumnType {
  /** A boolean. */
  BOOLEAN,
  /** A 32-bit integer. */
  INT,
  /** A 64-bit integer. */
  BIGINT,
  /** A 32-bit floating-point number. */
  REAL,
  /** A 64-bit floating-point number. */
  DOUBLE,
  /** Text. A column with a length holds at most that many characters. */
  TEXT,
  /** Binary data. A column with a length holds at most that many bytes. */
  BYTES,
  /** A UUID. */
  UUID,
  /** A point in time with microsecond precision. */
  TIMESTAMP,
  /** A JSON document. */
  JSON
}
