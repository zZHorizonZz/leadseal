package dev.horizon.leadseal;

/** Thrown when a schema, a migration history or a migration cannot be read, written or rendered. */
public class SchemaException extends RuntimeException {

  public SchemaException(String message) {
    super(message);
  }

  public SchemaException(String message, Throwable cause) {
    super(message, cause);
  }
}
