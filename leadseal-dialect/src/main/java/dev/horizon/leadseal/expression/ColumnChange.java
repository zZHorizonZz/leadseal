package dev.horizon.leadseal.expression;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Whether an update changes a column: the old and the new value differ, where {@code NULL} equals itself. It reads
 * the old and the new row, so only a trigger of an update has it.
 *
 * @param column the column name
 */
public record ColumnChange(String column) implements Condition {

  public ColumnChange {
    if (column == null || column.isBlank()) {
      throw new InvalidSchemaException("A change of an expression has no column.");
    }
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return environment.dialect().difference(environment.column(column, Row.OLD), environment.column(column,
        Row.NEW));
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of();
  }

  @Override
  public Set<ColumnReference> columns() {
    return new LinkedHashSet<>(List.of(new ColumnReference(column, Row.OLD), new ColumnReference(column, Row.NEW)));
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new ColumnChange(names.apply(column));
  }

  @Override
  public void validate(Table table, Set<Row> rows) {
    if (!rows.contains(Row.OLD) || !rows.contains(Row.NEW)) {
      throw new InvalidSchemaException("An expression of the table " + table.name() + " calls changed(), which needs the"
          + " old and the new row.");
    }
    columns().forEach(reference -> reference.validate(table, rows));
  }
}
