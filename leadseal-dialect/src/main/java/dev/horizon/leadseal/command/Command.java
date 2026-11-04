package dev.horizon.leadseal.command;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.Index;
import java.util.List;

/**
 * A statement of a migration script. The script ends the statement after the command writes it. The records of this
 * interface are the statements of standard SQL that the {@link Ddl} of a dialect gives by default.
 */
@FunctionalInterface
public interface Command {

  /**
   * Writes the statement.
   *
   * @param script the script
   */
  void render(ScriptWriter script);

  /**
   * {@code CREATE SCHEMA IF NOT EXISTS namespace}.
   *
   * @param namespace the namespace
   */
  record CreateSchema(String namespace) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE SCHEMA IF NOT EXISTS ").name(namespace);
    }
  }

  /**
   * {@code CREATE TABLE table (elements)}.
   *
   * @param table the table
   * @param elements the definitions of the columns and the constraints
   */
  record CreateTable(TableName table, List<Clause> elements) implements Command {

    public CreateTable {
      elements = List.copyOf(elements);
    }

    @Override
    public void render(ScriptWriter script) {
      script.append("CREATE TABLE ").table(table).append(" (");
      for (int i = 0; i < elements.size(); i++) {
        script.append(i == 0 ? "" : ", ").clause(elements.get(i));
      }
      script.append(")");
    }
  }

  /**
   * {@code DROP TABLE table}.
   *
   * @param table the table
   */
  record DropTable(TableName table) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP TABLE ").table(table);
    }
  }

  /**
   * {@code ALTER TABLE table RENAME TO name}.
   *
   * @param table the table
   * @param name the new name
   */
  record RenameTable(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" RENAME TO ").name(name);
    }
  }

  /**
   * {@code ALTER TABLE table RENAME CONSTRAINT origin TO target}.
   *
   * @param table the table
   * @param origin the name of the constraint
   * @param target the new name of the constraint
   */
  record RenameConstraint(TableName table, String origin, String target) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" RENAME CONSTRAINT ").name(origin).append(" TO ")
          .name(target);
    }
  }

  /**
   * {@code ALTER TABLE table ADD COLUMN definition}.
   *
   * @param table the table
   * @param definition the definition of the column
   */
  record AddColumn(TableName table, Clause definition) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" ADD COLUMN ").clause(definition);
    }
  }

  /**
   * {@code ALTER TABLE table DROP COLUMN column}.
   *
   * @param table the table
   * @param column the name of the column
   */
  record DropColumn(TableName table, String column) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" DROP COLUMN ").name(column);
    }
  }

  /**
   * {@code ALTER TABLE table RENAME COLUMN origin TO target}.
   *
   * @param table the table
   * @param origin the name of the column
   * @param target the new name of the column
   */
  record RenameColumn(TableName table, String origin, String target) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" RENAME COLUMN ").name(origin).append(" TO ").name(target);
    }
  }

  /**
   * {@code ALTER TABLE table ALTER COLUMN column action}, such as {@code SET NOT NULL}.
   *
   * @param table the table
   * @param column the name of the column
   * @param action the SQL of the action
   */
  record AlterColumn(TableName table, String column, String action) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ").name(column).append(" " + action);
    }
  }

  /**
   * {@code UPDATE table SET column = fill WHERE column IS NULL}, which gives the rows a value before the column
   * becomes required. The fill is the default of the column, or the zero of its type.
   *
   * @param table the table
   * @param column the column, as it is after the migration
   */
  record FillNulls(TableName table, Column column) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("UPDATE ").table(table).append(" SET ").name(column.name())
          .append(" = " + script.dialect().fill(column) + " WHERE ").name(column.name()).append(" IS NULL");
    }
  }

  /**
   * {@code ALTER TABLE table ADD constraint}.
   *
   * @param table the table
   * @param constraint the constraint
   */
  record AddConstraint(TableName table, Clause constraint) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" ADD ").clause(constraint);
    }
  }

  /**
   * {@code ALTER TABLE table DROP CONSTRAINT name}.
   *
   * @param table the table
   * @param name the name of the constraint
   */
  record DropConstraint(TableName table, String name) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("ALTER TABLE ").table(table).append(" DROP CONSTRAINT ").name(name);
    }
  }

  /**
   * {@code CREATE [UNIQUE] INDEX name ON table (columns) [WHERE condition]}.
   *
   * @param table the table
   * @param index the index
   */
  record CreateIndex(TableName table, Index index) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append(index.unique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ").name(index.name()).append(" ON ")
          .table(table).append(" (");
      for (int i = 0; i < index.columns().size(); i++) {
        String column = index.columns().get(i);
        script.append(i == 0 ? "" : ", ").name(column).append(index.descending().contains(column) ? " DESC" : "");
      }
      script.append(")" + index.rows().clause());
    }
  }

  /**
   * {@code DROP INDEX [namespace.]name}.
   *
   * @param table the table of the index
   * @param index the index
   */
  record DropIndex(TableName table, Index index) implements Command {

    @Override
    public void render(ScriptWriter script) {
      script.append("DROP INDEX ").name(table.namespace(), index.name());
    }
  }
}
