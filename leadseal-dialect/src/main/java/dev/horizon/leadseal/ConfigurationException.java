package dev.horizon.leadseal;

/**
 * Thrown when leadseal is set up wrong, such as a dialect name that no installed dialect has or a protoc plugin option
 * that does not exist. Fix the build configuration.
 */
public class ConfigurationException extends SchemaException {

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
