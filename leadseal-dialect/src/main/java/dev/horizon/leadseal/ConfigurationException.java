package dev.horizon.leadseal;

/** leadseal is set up wrong: no installed dialect has a name, no dialect is given, or an option of the protoc plugin is invalid. The build has to change. */
public final class ConfigurationException extends SchemaException {

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message what is wrong and how to fix it
   * @param cause the exception that revealed the problem
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
