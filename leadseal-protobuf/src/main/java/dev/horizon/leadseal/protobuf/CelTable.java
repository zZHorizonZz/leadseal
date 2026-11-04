package dev.horizon.leadseal.protobuf;

import dev.horizon.aquery.cel.Scope;
import dev.horizon.aquery.cel.Type.Scalar;
import dev.horizon.aquery.cel.expression.CelEnvironment;
import dev.horizon.aquery.cel.expression.CelGrammar;
import dev.horizon.aquery.cel.expression.CompiledExpression;
import dev.horizon.aquery.cel.expression.InvalidExpressionException;
import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A table as the expressions of its schema read it: in a subset of CEL, which {@link #parse} reads into a
 * {@link SqlExpression}.
 *
 * <p>
 * The text names the fields of the table, as in {@code price * quantity}, and in a trigger the fields of the new and
 * the old row, as in {@code new.total > old.total}. It can use:
 *
 * <ul>
 * <li>Constants: {@code 1}, {@code 1.5}, {@code 'text'}, {@code true}, {@code b'\x00'} and
 * {@code timestamp('2026-01-01T00:00:00Z')}.
 * <li>{@code == != < <= > >=}, {@code && || !}, {@code + - * / %} on numbers, {@code +} on texts, and
 * {@code value in [a, b]}.
 * <li>{@code condition ? a : b}, {@code has(field)}, and {@code field == null} or {@code field != null} on a field
 * that can be {@code NULL}.
 * <li>The functions {@code now()}, {@code uuid()}, {@code lower}, {@code upper}, {@code trim}, {@code length},
 * {@code abs}, {@code coalesce}, and {@code changed(field)} in a trigger.
 * </ul>
 *
 * <p>
 * CEL checks the types: {@code 'a' + 1} and {@code price * 2} on a double field are refused, as {@code 2} is an
 * integer; write {@code price * 2.0}. A divisor must be a constant that is not zero, since the databases disagree
 * on a division by zero. Fields of the types date, time and JSON cannot be read yet.
 *
 * <p>
 * A table cannot change. It is safe for use by several threads at the same time.
 */
public final class CelTable {

  private final Table table;
  private final Map<String, String> fields;
  private final Scope scope;
  private final CelGrammar grammar;

  /**
   * Reads the expressions of a table, whose text names the columns by their names.
   *
   * @param table the table
   */
  public CelTable(Table table) {
    this(table, table.columns().stream().collect(Collectors.toMap(Column::name, Column::name, (first, _) -> first,
        LinkedHashMap::new)));
  }

  /**
   * Reads the expressions of a table, whose text names the columns by other names, such as the names of the fields
   * of a protobuf message.
   *
   * @param table the table
   * @param fields the name of the column of each name that the text can use
   * @throws IllegalArgumentException if the table has no column of a name
   */
  public CelTable(Table table, Map<String, String> fields) {
    this.table = table;
    this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    Scope.Builder builder = Scope.builder(table.name());
    fields.forEach((field, name) -> declare(builder, field, column(name)));
    this.scope = builder.build();
    CelGrammar functions = CelGrammar.standard();
    for (CelFunction function : CelFunction.all()) {
      functions = functions.with(function);
    }
    this.grammar = functions;
  }

  /**
   * Reads an expression of the table.
   *
   * @param text the text, for example {@code quantity > 0 && state != 'CANCELLED'}
   * @param rows the rows that the expression can read: {@link Row#CURRENT} by the names of the fields, and
   * {@link Row#NEW} and {@link Row#OLD} as {@code new.field} and {@code old.field}
   * @param result the type that the value of the expression must have, for example a {@link BooleanType} for a
   * condition
   * @return the expression, which reads the columns by their names
   * @throws IllegalArgumentException if the rows do not have {@link Row#CURRENT}
   * @throws SchemaException if the text is not an expression of the table that gives a value of the type
   */
  public SqlExpression parse(String text, Set<Row> rows, Type result) {
    if (!rows.contains(Row.CURRENT)) {
      throw new IllegalArgumentException("An expression always reads the current row.");
    }
    CelTranslation translation = new CelTranslation(text);
    Scalar type = new CelScalar(result).scalar().orElseThrow(() -> new InvalidSchemaException(
        "An expression cannot give a value of the type " + result.example() + " yet."));
    SqlExpression expression = translation.expression(compile(translation, rows, type).root());
    try {
      expression.validate(table, rows);
    } catch (SchemaException invalid) {
      throw new InvalidSchemaException("The expression '" + text + "' is not valid. " + invalid.getMessage(), invalid);
    }
    return expression;
  }

  private CompiledExpression compile(CelTranslation translation, Set<Row> rows, Scalar type) {
    try {
      return grammar.compile(translation.text(), environment(rows), type);
    } catch (InvalidExpressionException invalid) {
      throw translation.refuse(reason(invalid));
    }
  }

  private CelEnvironment environment(Set<Row> rows) {
    CelEnvironment environment = CelEnvironment.of(scope);
    try {
      for (Row row : List.of(Row.NEW, Row.OLD)) {
        if (rows.contains(row)) {
          environment = environment.with(row.name().toLowerCase(Locale.ROOT), scope);
        }
      }
    } catch (IllegalArgumentException clash) {
      throw new InvalidSchemaException("The table " + table.name() + " has a field named new or old, so an expression of"
          + " a trigger cannot read it.", clash);
    }
    return environment;
  }

  private String reason(InvalidExpressionException invalid) {
    for (Map.Entry<String, String> field : fields.entrySet()) {
      CelScalar type = new CelScalar(column(field.getValue()).type());
      if (type.scalar().isEmpty() && invalid.getMessage().contains("undeclared reference to '" + field.getKey()
          + "'")) {
        return "the field " + field.getKey() + " has the type " + type.type() + ", which an expression cannot read"
            + " yet";
      }
    }
    return invalid.getMessage() + " (at character " + (invalid.position() + 1) + ")";
  }

  private void declare(Scope.Builder builder, String field, Column column) {
    new CelScalar(column.type()).declare(builder, field, column.name(), column.attribute(NotNull.class).isEmpty());
  }

  private Column column(String name) {
    return table.column(name).orElseThrow(() -> new IllegalArgumentException("The table " + table.name()
        + " has no column " + name + "."));
  }
}
