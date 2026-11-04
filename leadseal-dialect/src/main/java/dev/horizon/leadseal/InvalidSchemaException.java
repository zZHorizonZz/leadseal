package dev.horizon.leadseal;

/** The schema is invalid: a table, column, attribute, type or expression breaks a rule of leadseal, or a protobuf option is invalid. The schema has to change. */
public final class InvalidSchemaException extends SchemaException {

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  public InvalidSchemaException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message what is wrong and how to fix it
   * @param cause the exception that revealed the problem
   */
  public InvalidSchemaException(String message, Throwable cause) {
    super(message, cause);
  }
}
