package dev.horizon.leadseal;

import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Clause;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.migration.MigrationOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the SQL script of a migration in one dialect.
 *
 * <p>
 * The writer asks the {@link Ddl} of the dialect for the commands of the changes, with the schemas before and after
 * the migration, and renders each command as a statement.
 */
public final class ScriptWriter {

  private final Dialect dialect;
  private final Transition transition;
  private final String prefix;
  private final List<Script.Statement> statements = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();
  private final StringBuilder statement = new StringBuilder();
  private boolean transactional = true;
  private boolean isolated;

  public ScriptWriter(Dialect dialect, Schema before, Schema after) {
    this(dialect, before, after, MigrationOptions.DEFAULT_PREFIX);
  }

  public ScriptWriter(Dialect dialect, Schema before, Schema after, String prefix) {
    this.dialect = dialect;
    this.transition = new Transition(before, after);
    this.prefix = prefix == null ? "" : prefix;
  }

  /**
   * Writes the script of the changes of a migration.
   *
   * @param changes the changes of the migration
   * @return this writer
   * @throws SchemaException if the dialect cannot write a change
   */
  public ScriptWriter write(List<Change> changes) {
    changes.forEach(change -> change.warning().ifPresent(warnings::add));
    for (Command command : dialect.ddl().plan(changes, transition).commands()) {
      render(command).end();
    }
    return end();
  }

  /**
   * Gives the script that this writer has written.
   *
   * @return the script
   */
  public Script script() {
    return new Script(statements, transactional, warnings);
  }

  /**
   * Gives the SQL of the statements that this writer has written.
   *
   * @return the statements, in order, without semicolons
   */
  public List<String> statements() {
    return script().sql();
  }

  /**
   * Tells if the statements that this writer has written can run in a transaction.
   *
   * @return false if the statements must run outside a transaction, for example because they change a setting
   */
  public boolean transactional() {
    return transactional;
  }

  /**
   * Gives the text of the migration file of the statements that this writer has written.
   *
   * @return the file text
   * @see Script#text(Dialect)
   */
  public String text() {
    return script().text(dialect);
  }

  /**
   * Returns the name of a temporary object of the migration, such as a temporary table: the name with the prefix of
   * the options.
   *
   * @param name the name without the prefix, for example {@code check}
   * @return the name, for example {@code leadseal_check}
   */
  public String internal(String name) {
    return prefix + name;
  }

  /**
   * Returns the position of the current statement in the script. Each statement has a different position.
   *
   * @return the number of statements that the script has before the current one
   */
  public int position() {
    return statements.size();
  }

  /**
   * Returns the dialect.
   *
   * @return the dialect
   */
  public Dialect dialect() {
    return dialect;
  }

  /**
   * Renders a command.
   *
   * @param command the command
   * @return this writer
   */
  public ScriptWriter render(Command command) {
    command.render(this);
    return this;
  }

  /**
   * Writes a clause into the current statement.
   *
   * @param clause the clause
   * @return this writer
   */
  public ScriptWriter clause(Clause clause) {
    clause.write(this);
    return this;
  }

  /**
   * Appends SQL text as it is.
   *
   * @param sql the text to append
   * @return this writer
   */
  public ScriptWriter append(String sql) {
    statement.append(sql);
    return this;
  }

  /**
   * Appends a quoted identifier.
   *
   * @param identifier the identifier
   * @return this writer
   */
  public ScriptWriter name(String identifier) {
    return append(dialect.quote(identifier));
  }

  /**
   * Appends quoted identifiers, separated by commas.
   *
   * @param identifiers the identifiers
   * @return this writer
   */
  public ScriptWriter names(List<String> identifiers) {
    for (int i = 0; i < identifiers.size(); i++) {
      append(i == 0 ? "" : ", ").name(identifiers.get(i));
    }
    return this;
  }

  /**
   * Appends a name in a database schema: the quoted schema, a dot and the quoted name, or only the quoted name if the
   * schema is {@code null}.
   *
   * @param namespace the namespace
   * @param name the name
   * @return this writer
   */
  public ScriptWriter name(Namespace.Name namespace, String name) {
    namespace.map(text -> name(text).append("."), () -> this);
    return name(name);
  }

  /**
   * Appends the qualified name of a table: the quoted namespace, a dot and the quoted name, or only the quoted name for
   * the default namespace.
   *
   * @param name the table name
   * @return this writer
   */
  public ScriptWriter table(TableName name) {
    return name(name.namespace(), name.name());
  }

  /**
   * Marks the current statement as one that runs in a batch of its own. SQL Server, for example, accepts
   * {@code CREATE TRIGGER} only as the first statement of a batch. {@link #text()} separates the batch of the
   * statement from the statements before and after it, if the dialect has a {@linkplain Dialect#batchSeparator() batch
   * separator}. {@link #statements()} has each statement on its own already.
   *
   * @return this writer
   */
  public ScriptWriter isolate() {
    isolated = true;
    return this;
  }

  /**
   * Marks the migration as one that must run outside a transaction.
   *
   * @return this writer
   */
  public ScriptWriter outsideTransaction() {
    transactional = false;
    return this;
  }

  /**
   * Ends the current statement. A statement without text is skipped, and the next statement is not isolated.
   *
   * @return this writer
   */
  public ScriptWriter end() {
    if (!statement.isEmpty()) {
      statements.add(new Script.Statement(statement.toString(), isolated));
      statement.setLength(0);
    }
    isolated = false;
    return this;
  }
}
