package dev.horizon.leadseal;

/**
 * The database of a dialect cannot do what the schema or a change asks for, such as a namespace in SQLite or a new {@code IDENTITY} on an existing SQL Server column. The schema has to change, or the
 * dialect.
 */
public final class UnsupportedChangeException extends SchemaException {

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  public UnsupportedChangeException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message what is wrong and how to fix it
   * @param cause the exception that revealed the problem
   */
  public UnsupportedChangeException(String message, Throwable cause) {
    super(message, cause);
  }
}
