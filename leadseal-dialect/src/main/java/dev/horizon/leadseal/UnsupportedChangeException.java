package dev.horizon.leadseal;

/**
 * Thrown when a database cannot make a change, such as a namespace on SQLite or a new {@code IDENTITY} on an existing
 * SQL Server column. Change the schema, or leave out the dialect of that database.
 */
public class UnsupportedChangeException extends SchemaException {

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
