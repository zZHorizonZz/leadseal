package dev.horizon.leadseal;

import java.util.List;

/**
 * A check constraint of a table.
 *
 * @param name the constraint name
 * @param expression the SQL condition that every row must meet, for example {@code quantity >= 0}. Every dialect
 * writes it as it is.
 */
public record Check(String name, String expression) {

  private static final int HASH_LENGTH = 8;

  public Check {
    Names.require(name, "check name");
    Names.require(expression, "expression of the check '" + name + "'");
  }

  /**
   * Returns a check with a generated name: {@code ck_} followed by the table name and a hash of the expression.
   *
   * @param table the table name
   * @param expression the SQL condition
   * @return the check
   */
  public static Check of(String table, String expression) {
    return new Check(Names.generate("ck", table, List.of(Names.hash(expression).substring(0, HASH_LENGTH))),
        expression);
  }
}
