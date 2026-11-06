package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A generated column: the database writes the value of an expression of the other columns of its row into the column,
 * on each insert and update, and stores it. A generated column accepts {@code NULL}, and has no default, no identity,
 * no collation, and no key or index.
 *
 * <p>
 * A migration never changes a generated column in place: it drops the column and adds it again, which writes the values
 * of the expression into the rows.
 *
 * @param expression the value, which reads the columns of the row as {@link Row#CURRENT}. It reads no generated
 * column, and gives the same value for the same row each time, so it calls no {@code now()} or {@code uuid()}.
 */
public record Generated(SqlExpression expression) implements ColumnAttribute {

  public Generated {
    Objects.requireNonNull(expression, "expression");
  }

  /**
   * Gives the names of the columns that the expression reads.
   *
   * @return the names
   */
  public Set<String> reads() {
    return expression.columns().stream().map(ColumnReference::name).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public void validate(Column column) {
    if (!column.nullable() || column.attribute(Default.class).isPresent() || column.attribute(Identity.class)
        .isPresent() || !column.attributes(Collation.class).isEmpty()) {
      throw new InvalidSchemaException("The generated column '" + column.name() + "' must accept NULL, and have no"
          + " default, no identity and no collation.");
    }
    if (!expression.deterministic()) {
      throw new InvalidSchemaException("The generated column '" + column.name() + "' calls now() or uuid(), whose value"
          + " changes each time. A generated column needs the same value for the same row.");
    }
    if (!expression.foreignColumns().isEmpty()) {
      throw new InvalidSchemaException("The generated column '" + column.name() + "' reads another table. A generated"
          + " column reads only its own row.");
    }
  }

  @Override
  public void validate(Table table, Column column) {
    try {
      expression.validate(table, Set.of(Row.CURRENT));
    } catch (SchemaException invalid) {
      throw new InvalidSchemaException("The generated column '" + table.name() + "." + column.name() + "' is not valid. "
          + invalid.getMessage(), invalid);
    }
    for (String name : reads()) {
      if (table.column(name).orElseThrow().attribute(Generated.class).isPresent()) {
        throw new InvalidSchemaException("The generated column '" + table.name() + "." + column.name() + "' reads the"
            + " generated column " + name + ". A generated column reads only columns that are not generated.");
      }
    }
    boolean inKey = table.primaryKey().columns().contains(column.name())
        || table.attributes(Index.class).stream().anyMatch(index -> index.columns().contains(column.name()))
        || table.attributes(ForeignKey.class).stream().anyMatch(key -> key.columns().contains(column.name()));
    if (inKey) {
      throw new InvalidSchemaException("The generated column '" + table.name() + "." + column.name() + "' is in a key or"
          + " an index. A generated column cannot be in a primary key, an index or a foreign key.");
    }
  }

  @Override
  public List<Command> add(Ddl ddl, ColumnTransition column) {
    throw new IllegalStateException("A migration adds a generated column again instead of adding its expression.");
  }

  @Override
  public List<Command> drop(Ddl ddl, ColumnTransition column) {
    throw new IllegalStateException("A migration drops a generated column instead of dropping its expression.");
  }
}
