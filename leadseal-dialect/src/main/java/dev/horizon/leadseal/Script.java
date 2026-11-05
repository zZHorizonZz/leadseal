package dev.horizon.leadseal;

import java.util.List;

/**
 * The SQL script of a migration in one dialect: its statements, in the order they run.
 *
 * @param statements the statements
 * @param transactional false if the script must run outside a transaction
 * @param warnings the descriptions of the data that the script deletes, such as {@code drops the table orders and its
 * rows}
 */
public record Script(List<Statement> statements, boolean transactional, List<String> warnings) {

  public Script {
    statements = List.copyOf(statements);
    warnings = List.copyOf(warnings);
  }

  /**
   * Gives the SQL of the statements.
   *
   * @return the SQL of each statement, without a semicolon
   */
  public List<String> sql() {
    return statements.stream().map(Statement::sql).toList();
  }

  /**
   * Gives the text of the migration file. A comment line for each warning starts the file, such as
   * {@code -- Warning: drops the table orders and its rows.}, and a blank line follows the comments. Each statement
   * ends with a semicolon, and a blank line separates two statements. If the dialect has a
   * {@linkplain Dialect#batchSeparator() batch separator}, a line with the separator ends the batch before and the
   * batch of each statement that runs in a batch of its own.
   *
   * @param dialect the dialect of the script
   * @return the file text
   */
  public String text(Dialect dialect) {
    String separator = dialect.batchSeparator().map(line -> line + "\n\n").orElse("\n");
    StringBuilder text = new StringBuilder();
    warnings.forEach(warning -> text.append("-- Warning: ").append(warning).append(".\n"));
    if (!warnings.isEmpty() && !statements.isEmpty()) {
      text.append("\n");
    }
    for (int i = 0; i < statements.size(); i++) {
      if (i > 0) {
        text.append(statements.get(i).isolates(statements.get(i - 1)) ? separator : "\n");
      }
      text.append(statements.get(i).sql()).append(";\n");
    }
    return text.toString();
  }

  /**
   * A statement of a script.
   *
   * @param sql the SQL, without a semicolon
   * @param isolated true if the statement runs in a batch of its own, as SQL Server runs {@code CREATE TRIGGER}
   */
  public record Statement(String sql, boolean isolated) {

    /**
     * Tells if a batch ends between another statement and this statement, which follows it.
     *
     * @param previous the statement before this one
     * @return true if either statement runs in a batch of its own
     */
    public boolean isolates(Statement previous) {
      return isolated || previous.isolated;
    }
  }
}
