package dev.horizon.leadseal.expression;

/**
 * An expression that SQL writes as a condition, such as a comparison. Where SQL expects a value, the dialect turns
 * the condition into one.
 */
public sealed interface Condition extends SqlExpression permits Not, Comparison, Logic, IsNull, In, ColumnChange {

  /**
   * Writes this condition as SQL writes it.
   *
   * @param environment the dialect and the rows that the condition reads
   * @return the SQL
   */
  String sql(SqlEnvironment environment);

  @Override
  default String condition(SqlEnvironment environment) {
    return sql(environment);
  }

  @Override
  default String value(SqlEnvironment environment) {
    return environment.dialect().value(sql(environment));
  }
}
