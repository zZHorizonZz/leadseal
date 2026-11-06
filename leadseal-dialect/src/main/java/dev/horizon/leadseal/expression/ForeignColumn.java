package dev.horizon.leadseal.expression;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.Index;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * A column of the row that a key of a row references in another table, such as the name of the customer of an order.
 * It reads one row of the other table, and is {@code NULL} when the key is {@code NULL} or no row has it.
 *
 * @param key the column that holds the key, in the row that the expression reads
 * @param table the other table
 * @param reference the column of the other table that the key matches, such as its primary key
 * @param column the column of the other table that the expression reads
 */
public record ForeignColumn(ColumnReference key, TableName table, String reference, String column) implements Value {

  public ForeignColumn {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(table, "table");
    if (reference == null || reference.isBlank() || column == null || column.isBlank()) {
      throw new InvalidSchemaException("A foreign column of an expression has no name.");
    }
  }

  @Override
  public String sql(SqlEnvironment environment) {
    String row = environment.dialect().quote("r");
    return "(SELECT " + row + "." + environment.dialect().quote(column) + " FROM " + environment.dialect().quote(table)
        + " AS " + row + " WHERE " + row + "." + environment.dialect().quote(reference) + " = " + key.sql(environment)
        + ")";
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(key);
  }

  @Override
  public List<ForeignColumn> foreignColumns() {
    return List.of(this);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new ForeignColumn(new ColumnReference(names.apply(key.name()), key.row()), table, reference, column);
  }

  @Override
  public void validate(Table owner, Set<Row> rows) {
    key.validate(owner, rows);
  }

  /**
   * Checks that the other table has the columns that this expression reads.
   *
   * @param schema the schema of both tables
   * @throws SchemaException if the other table or one of its columns does not exist
   */
  public void validate(Schema schema) {
    Table target = schema.table(table).orElseThrow(() -> new InvalidSchemaException("An expression reads the table "
        + table + ", which the schema does not have."));
    for (String name : List.of(reference, column)) {
      if (target.column(name).isEmpty()) {
        throw new InvalidSchemaException("An expression reads the column " + table + "." + name + ", which the table"
            + " does not have.");
      }
    }
    boolean unique = target.primaryKey().columns().equals(List.of(reference)) || target.attributes(Index.class).stream()
        .anyMatch(index -> index.unique() && !index.rows().partial() && index.columns().equals(List.of(reference)));
    if (!unique) {
      throw new InvalidSchemaException("An expression reads the row of " + table + " whose " + reference + " matches a"
          + " key, but " + reference + " is neither the primary key nor a unique index, so it can match many rows.");
    }
  }
}
