package dev.horizon.leadseal;

/**
 * The name of a table in its namespace.
 *
 * @param namespace the namespace, a database schema, or {@code null} for the default namespace of the connection
 * @param name the table name, unique in its namespace
 */
public record TableName(String namespace, String name) {

  public TableName {
    if (name == null || name.isBlank()) {
      throw new SchemaException("A table name is empty.");
    }
    if (namespace != null && namespace.isBlank()) {
      throw new SchemaException("The namespace of the table '" + name + "' is empty.");
    }
  }

  /**
   * Returns the name of a table in the default namespace.
   *
   * @param name the table name
   * @return the table name
   */
  public static TableName of(String name) {
    return new TableName(null, name);
  }

  /**
   * Returns the name of a table with another name in the same namespace.
   *
   * @param name the new table name
   * @return the table name
   */
  public TableName withName(String name) {
    return new TableName(namespace, name);
  }

  /**
   * Returns the name of the same table in another namespace.
   *
   * @param namespace the namespace, or {@code null} for the default namespace
   * @return the table name
   */
  public TableName inNamespace(String namespace) {
    return new TableName(namespace, name);
  }

  @Override
  public String toString() {
    return namespace == null ? name : namespace + "." + name;
  }
}
