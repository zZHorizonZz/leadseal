package dev.horizon.leadseal;

/**
 * Thrown when a schema breaks a rule, such as a table without a primary key or a protobuf option with a wrong value.
 * Fix the schema.
 */
public class InvalidSchemaException extends SchemaException {

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
