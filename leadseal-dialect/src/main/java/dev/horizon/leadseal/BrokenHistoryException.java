package dev.horizon.leadseal;

/**
 * The migration history does not hold together: a migration does not follow the one before it, or a migration or snapshot file is missing, unreadable or of another format version. The migration
 * directory has to be repaired.
 */
public final class BrokenHistoryException extends SchemaException {

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
