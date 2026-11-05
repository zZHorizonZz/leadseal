package dev.horizon.leadseal;

/**
 * Thrown when leadseal cannot read a schema or write a migration. Each subclass says who has to fix the problem:
 * {@link InvalidSchemaException}, {@link UnsupportedChangeException}, {@link BrokenHistoryException} or
 * {@link ConfigurationException}.
 */
public abstract class SchemaException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  protected SchemaException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message what is wrong and how to fix it
   * @param cause the exception that revealed the problem
   */
  protected SchemaException(String message, Throwable cause) {
    super(message, cause);
  }
}
