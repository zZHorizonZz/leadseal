package dev.horizon.leadseal;

import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.CommandFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Writes the SQL script of a migration in one dialect.
 *
 * <p>
 * The writer asks the dialect's {@link CommandFactory} for the commands of the changes and renders each command. It
 * gives commands the schemas before and after the migration, so that a command can look up the full definition of its
 * table.
 */
public final class ScriptWriter {

  private final Dialect dialect;
  private final Schema before;
  private final Schema after;
  private final String prefix;
  private final List<String> statements = new ArrayList<>();
  private final StringBuilder statement = new StringBuilder();
  private boolean transactional = true;

  public ScriptWriter(Dialect dialect, Schema before, Schema after) {
    this(dialect, before, after, MigrationOptions.DEFAULT_PREFIX);
  }

  public ScriptWriter(Dialect dialect, Schema before, Schema after, String prefix) {
    this.dialect = dialect;
    this.before = before;
    this.after = after;
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
    for (Command command : commands().plan(changes, this)) {
      render(command).end();
    }
    return end();
  }

  /**
   * Returns the written statements.
   *
   * @return the statements, in order, without trailing semicolons
   */
  public List<String> statements() {
    return List.copyOf(statements);
  }

  /**
   * Returns whether the written statements can run in a transaction.
   *
   * @return {@code false} if the statements must run outside a transaction, for example because they change a setting
   * that a transaction ignores
   */
  public boolean transactional() {
    return transactional;
  }

  /**
   * Returns the written statements as the text of a migration file. Each statement ends with a semicolon and a blank
   * line separates two statements.
   *
   * @return the file text
   */
  public String text() {
    return String.join(";\n\n", statements) + (statements.isEmpty() ? "" : ";\n");
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
   * Returns the command factory of the dialect.
   *
   * @return the command factory
   */
  public CommandFactory commands() {
    return dialect.commands();
  }

  /**
   * Returns the schema before the migration.
   *
   * @return the schema
   */
  public Schema before() {
    return before;
  }

  /**
   * Returns the schema after the migration.
   *
   * @return the schema
   */
  public Schema after() {
    return after;
  }

  /**
   * Returns a table of the schema before the migration.
   *
   * @param name the table name
   * @return the table
   * @throws SchemaException if the schema before the migration has no such table
   */
  public Table tableBefore(TableName name) {
    return before.table(name).orElseThrow(() -> new SchemaException("The table " + name
        + " is not in the schema before the migration."));
  }

  /**
   * Returns a table of the schema after the migration.
   *
   * @param name the table name
   * @return the table
   * @throws SchemaException if the schema after the migration has no such table
   */
  public Table tableAfter(TableName name) {
    return after.table(name).orElseThrow(() -> new SchemaException("The table " + name
        + " is not in the schema after the migration."));
  }

  /**
   * Returns a column of the schema after the migration.
   *
   * @param table the table name
   * @param column the column name
   * @return the column
   * @throws SchemaException if the schema after the migration has no such column
   */
  public Column columnAfter(TableName table, String column) {
    return tableAfter(table).column(column).orElseThrow(() -> new SchemaException("The column " + table + "."
        + column + " is not in the schema after the migration."));
  }

  /**
   * Returns a table as it was before the migration, found by its id.
   *
   * @param table the table name after the migration
   * @return the table, or an empty optional if the table is new
   */
  public Optional<Table> previous(TableName table) {
    return before.tableById(tableAfter(table).id());
  }

  /**
   * Returns a column as it was before the migration, found by the ids of its table and of the column.
   *
   * @param table the table name after the migration
   * @param column the column name after the migration
   * @return the column, or an empty optional if the table or the column is new
   */
  public Optional<Column> columnBefore(TableName table, String column) {
    String id = columnAfter(table, column).id();
    return previous(table).flatMap(old -> old.columnById(id));
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
   * @param schema the schema, or {@code null}
   * @param name the name
   * @return this writer
   */
  public ScriptWriter qualified(String schema, String name) {
    if (schema != null) {
      name(schema).append(".");
    }
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
    return qualified(name.namespace(), name.name());
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
   * Ends the current statement. A statement without text is skipped.
   *
   * @return this writer
   */
  public ScriptWriter end() {
    if (!statement.isEmpty()) {
      statements.add(statement.toString());
      statement.setLength(0);
    }
    return this;
  }
}
