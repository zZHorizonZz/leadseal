package dev.horizon.leadseal.expression;

import java.util.Locale;

/** The row whose columns an expression reads. */
public enum Row {
  /** The row of a check or a computed column. In a trigger, the new row. */
  CURRENT,
  /** The row after an insert or an update, in a trigger. */
  NEW,
  /** The row before an update or a delete, in a trigger. */
  OLD;

  /**
   * Gives the name of the row in a message.
   *
   * @return the name, for example {@code old row}
   */
  public String describe() {
    return name().toLowerCase(Locale.ROOT) + " row";
  }
}
