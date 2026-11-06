package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Mutation;
import dev.horizon.leadseal.attribute.Trigger;
import dev.horizon.leadseal.attribute.Triggers;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlEnvironment;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The statements of triggers. Each database writes triggers in its own way, and standard SQL has no form that they
 * share, so each dialect gives its own.
 */
public interface TriggerDdl {

  /**
   * Gives the statements that these statements are part of.
   *
   * @return the statements of the dialect
   */
  Ddl ddl();

  /**
   * Gives the commands that create the database triggers of the rewrites and the triggers of a table, named
   * {@link Triggers#name} and {@link Triggers#rewriteName}.
   *
   * @param table the table, which has the triggers after the migration
   * @param triggers the rewrites and the triggers
   * @return the commands
   */
  List<Command> createTriggers(TableTransition table, Triggers triggers);

  /**
   * Gives the commands that drop the database triggers of the rewrites and the triggers of a table.
   *
   * @param table the table, which has the triggers before the migration
   * @param triggers the rewrites and the triggers
   * @return the commands
   */
  List<Command> dropTriggers(TableTransition table, Triggers triggers);

  /**
   * Gives the environment of the expressions of a database trigger that runs for each row and names its rows
   * {@code NEW} and {@code OLD}.
   *
   * @param mutation the mutation of the trigger
   * @return the environment, which reads {@link Row#CURRENT} as {@code NEW} on an insert or an update and as
   * {@code OLD} on a delete
   */
  default SqlEnvironment rowEnvironment(Mutation mutation) {
    return SqlEnvironment.of(ddl().dialect()).with(Row.CURRENT, mutation.rows().contains(Row.NEW) ? "NEW" : "OLD")
        .with(Row.NEW, "NEW").with(Row.OLD, "OLD");
  }

  /**
   * Writes the insert of a trigger that runs for each row.
   *
   * @param insert the insert
   * @param environment the environment of the row of the trigger
   * @return the statement, without a semicolon. The default is {@code INSERT INTO ... VALUES (...)}.
   */
  default String insert(Trigger.Insert insert, SqlEnvironment environment) {
    Dialect dialect = ddl().dialect();
    return "INSERT INTO " + dialect.quote(insert.table()) + " (" + insert.values().stream().map(value -> dialect.quote(
        value.column())).collect(Collectors.joining(", ")) + ") VALUES (" + insert.values().stream().map(
            value -> value
                .expression().value(environment))
            .collect(Collectors.joining(", "))
        + ")";
  }

  /**
   * Writes the update of a trigger that runs for each row. The expressions read the other table as
   * {@link Row#CURRENT}.
   *
   * @param update the update
   * @param environment the environment of the row of the trigger
   * @return the statement, without a semicolon. The default is {@code UPDATE ... SET ... WHERE ...}.
   */
  default String update(Trigger.Update update, SqlEnvironment environment) {
    Dialect dialect = ddl().dialect();
    SqlEnvironment target = environment.with(Row.CURRENT, dialect.quote(update.table()));
    return "UPDATE " + dialect.quote(update.table()) + " SET " + update.values().stream().map(value -> dialect.quote(
        value.column()) + " = " + value.expression().value(target)).collect(Collectors.joining(", ")) + " WHERE "
        + update.where().condition(target);
  }

  /**
   * Writes the delete of a trigger that runs for each row. The condition reads the other table as
   * {@link Row#CURRENT}.
   *
   * @param delete the delete
   * @param environment the environment of the row of the trigger
   * @return the statement, without a semicolon. The default is {@code DELETE FROM ... WHERE ...}.
   */
  default String delete(Trigger.Delete delete, SqlEnvironment environment) {
    Dialect dialect = ddl().dialect();
    return "DELETE FROM " + dialect.quote(delete.table()) + " WHERE " + delete.where().condition(environment.with(
        Row.CURRENT, dialect.quote(delete.table())));
  }
}
