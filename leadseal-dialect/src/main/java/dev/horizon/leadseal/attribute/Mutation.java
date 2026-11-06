package dev.horizon.leadseal.attribute;

import com.fasterxml.jackson.annotation.JsonValue;
import dev.horizon.leadseal.expression.Row;
import java.util.Locale;
import java.util.Set;

/** A mutation of the rows of a table: an insert, an update or a delete. A {@link Trigger} runs on some of them. */
public enum Mutation {
  /** An insert, whose expressions read the new row. */
  INSERT(Set.of(Row.CURRENT, Row.NEW)),
  /** An update, whose expressions read the new and the old row. */
  UPDATE(Set.of(Row.CURRENT, Row.NEW, Row.OLD)),
  /** A delete, whose expressions read the old row. */
  DELETE(Set.of(Row.CURRENT, Row.OLD));

  private final Set<Row> rows;

  Mutation(Set<Row> rows) {
    this.rows = rows;
  }

  /**
   * Gives the rows that an expression of this mutation reads. {@link Row#CURRENT} is the new row of an insert or an
   * update, and the old row of a delete.
   *
   * @return the rows, for example {@link Row#NEW} and {@link Row#OLD} for an update
   */
  public Set<Row> rows() {
    return rows;
  }

  /**
   * Gives the name of this mutation in JSON and in names.
   *
   * @return the name, for example {@code insert}
   */
  @JsonValue
  public String text() {
    return name().toLowerCase(Locale.ROOT);
  }
}
