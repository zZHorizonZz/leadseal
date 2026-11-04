package dev.horizon.leadseal;

/**
 * A problem that stops leadseal from reading a schema or writing a migration. Each kind of problem has its own
 * subclass, so a caller can tell who has to fix it: {@link InvalidSchemaException}, {@link UnsupportedChangeException},
 * {@link BrokenHistoryException} or {@link ConfigurationException}.
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
