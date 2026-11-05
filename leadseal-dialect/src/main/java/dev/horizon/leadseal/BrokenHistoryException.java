package dev.horizon.leadseal;

/**
 * Thrown when the migration directory is broken: a migration does not follow the one before it, or a migration or
 * snapshot file is missing, unreadable or in an unknown format. Repair the migration directory.
 */
public class BrokenHistoryException extends SchemaException {

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  public BrokenHistoryException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message what is wrong and how to fix it
   * @param cause the exception that revealed the problem
   */
  public BrokenHistoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
