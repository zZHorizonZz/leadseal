package dev.horizon.leadseal.expression;

/**
 * An expression that SQL writes as a value, such as a column. Where SQL expects a condition, the dialect turns the
 * value into one.
 */
public sealed interface Value extends SqlExpression permits ColumnReference, Literal, Negative, Arithmetic, Conditional,
    Call {

  /**
   * Writes this value as SQL writes it.
   *
   * @param environment the dialect and the rows that the value reads
   * @return the SQL
   */
  String sql(SqlEnvironment environment);

  @Override
  default String condition(SqlEnvironment environment) {
    return environment.dialect().condition(sql(environment));
  }

  @Override
  default String value(SqlEnvironment environment) {
    return sql(environment);
  }
}
