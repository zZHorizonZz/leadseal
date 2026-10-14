package dev.horizon.leadseal;

/**
 * Sets or removes the comment of a column: {@code COMMENT ON COLUMN}. Dialects that write comments in the column
 * definition decorate this node away.
 *
 * @param table the table name, in the schema after the migration
 * @param column the column name
 * @param from the comment before, or {@code null}
 * @param to the comment after, or {@code null} to remove it
 */
public record ColumnComment(String table, String column, String from, String to) implements Node {

  @Override
  public void render(DdlWriter sql) {
    sql.append("COMMENT ON COLUMN ").table(table).append(".").name(column)
        .append(" IS " + (to == null ? "NULL" : sql.dialect().string(to)));
  }
}
