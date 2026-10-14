package dev.horizon.leadseal;

import java.util.List;

/**
 * The SQL statements of one migration in one dialect.
 *
 * @param statements the statements, in order, without trailing semicolons
 * @param transactional {@code false} if the statements must run outside a transaction, for example because they change
 * a setting that a transaction ignores
 */
public record SqlScript(List<String> statements, boolean transactional) {

  public SqlScript {
    statements = List.copyOf(statements);
  }

  public SqlScript(List<String> statements) {
    this(statements, true);
  }

  /**
   * Returns the statements as the text of a migration file. Each statement ends with a semicolon and a blank line
   * separates two statements.
   *
   * @return the file text
   */
  public String text() {
    return String.join(";\n\n", statements) + (statements.isEmpty() ? "" : ";\n");
  }
}
