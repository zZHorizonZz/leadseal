package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A rewrite: the value that the database writes into a column on each update of a row, in place of the value that
 * the update writes. An insert writes no rewrite: the default of the column gives its value.
 *
 * @param column the name of the column
 * @param expression the value, which reads {@link Row#CURRENT} and {@link Row#NEW} as the row that the update writes,
 * and {@link Row#OLD} as the row before the update. A column that another rewrite writes has its rewritten value.
 */
public record Rewrite(String column, SqlExpression expression) {

  public Rewrite {
    Objects.requireNonNull(expression, "expression");
    if (column == null || column.isBlank()) {
      throw new InvalidSchemaException("A rewrite has no column.");
    }
  }

  /**
   * Tells if this rewrite reads the new value of the column of another rewrite.
   *
   * @param other the other rewrite
   * @return true if this rewrite reads the column of the other rewrite in the row that the update writes
   */
  public boolean reads(Rewrite other) {
    return !other.column.equals(column) && expression.columns().stream()
        .anyMatch(reference -> reference.name().equals(other.column) && reference.row() != Row.OLD);
  }

  /**
   * Gives the columns of its table that this rewrite writes and reads.
   *
   * @return the names of the columns
   */
  public Set<String> reads() {
    Set<String> all = new LinkedHashSet<>(List.of(column));
    expression.columns().stream().map(ColumnReference::name).forEach(all::add);
    return all;
  }

  void validate(Table table) {
    if (table.column(column).isEmpty()) {
      throw new InvalidSchemaException("A rewrite of the table " + table.name() + " writes the column " + column
          + ", which the table does not have.");
    }
    if (table.primaryKey().columns().contains(column)) {
      throw new InvalidSchemaException("A rewrite of the table " + table.name() + " writes the column " + column
          + ", which is part of the primary key. A rewrite cannot change the key of a row.");
    }
    if (table.column(column).orElseThrow().attribute(Generated.class).isPresent()) {
      throw new InvalidSchemaException("A rewrite of the table " + table.name() + " writes the column " + column
          + ", which is a generated column. The database writes its values from its expression.");
    }
    for (String read : reads()) {
      if (table.column(read).flatMap(own -> own.attribute(Generated.class)).isPresent()) {
        throw new InvalidSchemaException("The rewrite of the column " + table.name() + "." + column + " reads the"
            + " generated column " + read + ", whose value the database writes only after the rewrites. Read the"
            + " columns of its expression instead.");
      }
    }
    try {
      expression.validate(table, Mutation.UPDATE.rows());
    } catch (SchemaException invalid) {
      throw new InvalidSchemaException("The rewrite of the column " + table.name() + "." + column + " is not valid. "
          + invalid.getMessage(), invalid);
    }
  }
}
