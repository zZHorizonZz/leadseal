package dev.horizon.leadseal;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a migration into SQL statements.
 *
 * <p>
 * Render every node through {@link #render(Node)}. That method passes the node to the dialect's decorator first. The
 * writer also gives nodes the schema before and after the migration, so that an operation can look up the full
 * definition of its table.
 */
public final class DdlWriter {

  private final Dialect dialect;
  private final Schema before;
  private final Schema after;
  private final List<String> statements = new ArrayList<>();
  private final StringBuilder statement = new StringBuilder();
  private boolean transactional = true;

  public DdlWriter(Dialect dialect, Schema before, Schema after) {
    this.dialect = dialect;
    this.before = before;
    this.after = after;
  }

  /**
   * Renders a change between two schemas.
   *
   * @param dialect the dialect
   * @param operations the operations of the change
   * @param before the schema before the change
   * @param after the schema after the change
   * @return the statements
   */
  public static SqlScript script(Dialect dialect, List<Operation> operations, Schema before, Schema after) {
    DdlWriter sql = new DdlWriter(dialect, before, after);
    sql.render(new Change(operations));
    return sql.script();
  }

  /**
   * Returns the dialect that this writer renders for.
   *
   * @return the dialect
   */
  public Dialect dialect() {
    return dialect;
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
  public Table tableBefore(String name) {
    return before.table(name).orElseThrow(() -> new SchemaException("The table '" + name
        + "' is not in the schema before the migration."));
  }

  /**
   * Returns a table of the schema after the migration.
   *
   * @param name the table name
   * @return the table
   * @throws SchemaException if the schema after the migration has no such table
   */
  public Table tableAfter(String name) {
    return after.table(name).orElseThrow(() -> new SchemaException("The table '" + name
        + "' is not in the schema after the migration."));
  }

  /**
   * Decorates a node with the dialect's decorator and renders the result.
   *
   * @param node the node to render
   * @return this writer
   */
  public DdlWriter render(Node node) {
    dialect.decorator().decorate(node).render(this);
    return this;
  }

  /**
   * Appends SQL text as it is.
   *
   * @param sql the text to append
   * @return this writer
   */
  public DdlWriter append(String sql) {
    statement.append(sql);
    return this;
  }

  /**
   * Appends a quoted identifier.
   *
   * @param identifier the identifier
   * @return this writer
   */
  public DdlWriter name(String identifier) {
    return append(dialect.quote(identifier));
  }

  /**
   * Appends quoted identifiers, separated by commas.
   *
   * @param identifiers the identifiers
   * @return this writer
   */
  public DdlWriter names(List<String> identifiers) {
    for (int i = 0; i < identifiers.size(); i++) {
      append(i == 0 ? "" : ", ").name(identifiers.get(i));
    }
    return this;
  }

  /**
   * Appends a column definition: the quoted name, the database type and {@code NOT NULL} for a column that does not
   * accept {@code NULL}.
   *
   * @param table the table of the column
   * @param column the column
   * @return this writer
   */
  public DdlWriter column(Table table, Column column) {
    name(column.name()).append(" " + dialect.type(table, column));
    return column.nullable() ? this : append(" NOT NULL");
  }

  /**
   * Marks the migration as one that must run outside a transaction.
   *
   * @return this writer
   */
  public DdlWriter outsideTransaction() {
    transactional = false;
    return this;
  }

  /**
   * Ends the current statement. A statement without text is skipped.
   *
   * @return this writer
   */
  public DdlWriter end() {
    if (!statement.isEmpty()) {
      statements.add(statement.toString());
      statement.setLength(0);
    }
    return this;
  }

  /**
   * Ends the current statement and returns all statements.
   *
   * @return the statements
   */
  public SqlScript script() {
    end();
    return new SqlScript(statements, transactional);
  }
}
