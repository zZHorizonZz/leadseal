package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.attribute.Check;
import dev.horizon.leadseal.attribute.ForeignKey;
import dev.horizon.leadseal.attribute.Index;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.command.Command.AddConstraint;
import dev.horizon.leadseal.command.Command.CreateIndex;
import dev.horizon.leadseal.command.Command.DropConstraint;
import dev.horizon.leadseal.command.Command.DropIndex;
import java.util.List;

/** The statements of primary keys, indexes, checks and foreign keys. Each has a default of standard SQL. */
@FunctionalInterface
public interface ConstraintDdl {

  /**
   * Gives the statements that these statements are part of.
   *
   * @return the statements of the dialect
   */
  Ddl ddl();

  /**
   * Gives the commands that change the primary key of a table.
   *
   * @param table the table
   * @param origin the primary key before the migration
   * @param target the primary key after the migration
   * @return the commands. The default drops the key and adds the new one.
   */
  default List<Command> alterPrimaryKey(TableTransition table, PrimaryKey origin, PrimaryKey target) {
    return List.of(new DropConstraint(table.name(), origin.name()),
        new AddConstraint(table.name(), new Clause.PrimaryKeyConstraint(target)));
  }

  /**
   * Gives the commands that create an index.
   *
   * @param table the table
   * @param index the index
   * @return the commands. The default is {@code CREATE INDEX}.
   */
  default List<Command> addIndex(TableTransition table, Index index) {
    return List.of(new CreateIndex(table.name(), index));
  }

  /**
   * Gives the commands that drop an index.
   *
   * @param table the table
   * @param index the index
   * @return the commands. The default is {@code DROP INDEX}.
   */
  default List<Command> dropIndex(TableTransition table, Index index) {
    return List.of(new DropIndex(table.name(), index));
  }

  /**
   * Gives the commands that add a check to a table.
   *
   * @param table the table
   * @param check the check
   * @return the commands. The default adds the constraint.
   */
  default List<Command> addCheck(TableTransition table, Check check) {
    return List.of(new AddConstraint(table.name(), new Clause.CheckConstraint(check)));
  }

  /**
   * Gives the commands that drop a check of a table.
   *
   * @param table the table
   * @param check the check
   * @return the commands. The default drops the constraint.
   */
  default List<Command> dropCheck(TableTransition table, Check check) {
    return List.of(new DropConstraint(table.name(), check.name()));
  }

  /**
   * Gives the commands that add a foreign key to a table.
   *
   * @param table the table
   * @param key the foreign key
   * @return the commands. The default adds the constraint.
   */
  default List<Command> addForeignKey(TableTransition table, ForeignKey key) {
    return List.of(new AddConstraint(table.name(), foreignKey(key)));
  }

  /**
   * Gives the commands that drop a foreign key of a table.
   *
   * @param table the table
   * @param key the foreign key
   * @return the commands. The default drops the constraint.
   */
  default List<Command> dropForeignKey(TableTransition table, ForeignKey key) {
    return List.of(new DropConstraint(table.name(), key.name()));
  }

  /**
   * Gives the clause of a foreign key.
   *
   * @param key the foreign key
   * @return the clause. The default is the standard clause.
   */
  default Clause foreignKey(ForeignKey key) {
    return new Clause.ForeignKeyConstraint(key);
  }
}
