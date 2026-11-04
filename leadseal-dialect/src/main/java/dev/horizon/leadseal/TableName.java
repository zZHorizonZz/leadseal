package dev.horizon.leadseal;

/**
 * The name of a table in its namespace.
 *
 * @param namespace the namespace, a database schema or the default namespace of the connection
 * @param name the table name, unique in its namespace
 */
public record TableName(Namespace.Name namespace, String name) {

  public TableName {
    namespace = namespace == null ? Namespace.Name.DEFAULT : namespace;
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("A table name is empty.");
    }
  }

  /**
   * Gives the name of a table in a namespace.
   *
   * @param namespace the name of the namespace, such as {@code billing}
   * @param name the table name
   */
  public TableName(String namespace, String name) {
    this(Namespace.Name.of(namespace), name);
  }

  /**
   * Gives the name of a table in the default namespace.
   *
   * @param name the table name
   * @return the table name
   */
  public static TableName of(String name) {
    return new TableName(Namespace.Name.DEFAULT, name);
  }

  /**
   * Gives the name of a table with another name in the same namespace.
   *
   * @param name the new table name
   * @return the table name
   */
  public TableName withName(String name) {
    return new TableName(namespace, name);
  }

  /**
   * Gives the name of the same table in another namespace.
   *
   * @param namespace the namespace
   * @return the table name
   */
  public TableName inNamespace(Namespace.Name namespace) {
    return new TableName(namespace, name);
  }

  @Override
  public String toString() {
    return namespace.map(text -> text + "." + name, () -> name);
  }
}
