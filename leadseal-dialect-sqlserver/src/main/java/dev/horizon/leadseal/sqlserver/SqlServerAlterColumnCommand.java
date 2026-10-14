package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.CheckAttribute;
import dev.horizon.leadseal.attribute.DefaultAttribute;
import dev.horizon.leadseal.attribute.ForeignKeyAttribute;
import dev.horizon.leadseal.attribute.IndexAttribute;
import dev.horizon.leadseal.attribute.PrimaryKeyAttribute;
import dev.horizon.leadseal.command.Command;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ALTER TABLE ... ALTER COLUMN}, which sets the type, the collation and the nullability of a column together.
 * SQL Server cannot change a column while an index, a key, a check or a default uses it, so these are dropped before
 * the change and created again after it, with the default of the column after the migration.
 */
class SqlServerAlterColumnCommand extends Command {

  private final TableName table;
  private final String column;

  SqlServerAlterColumnCommand(TableName table, String column) {
    this.table = table;
    this.column = column;
  }

  @Override
  public void render(ScriptWriter sql) {
    Table after = sql.tableAfter(table);
    Table before = sql.previous(table).orElseThrow();
    Column to = sql.columnAfter(table, column);
    Column from = sql.columnBefore(table, column).orElseThrow();
    List<IndexAttribute> indexes = after.attributes(IndexAttribute.class).stream()
        .filter(index -> index.contains(column) && before.attributes().contains(index)).toList();
    List<CheckAttribute> checks = after.attributes(CheckAttribute.class).stream()
        .filter(before.attributes()::contains).toList();
    List<ForeignKey> keys = keys(sql, before);
    PrimaryKeyAttribute primaryKey = after.primaryKey();
    boolean keyed = before.primaryKey().columns().contains(from.name());
    for (ForeignKey key : keys) {
      sql.render(sql.commands().dropForeignKey(key.table(), key.key())).end();
    }
    for (IndexAttribute index : indexes) {
      sql.render(sql.commands().dropIndex(table, index)).end();
    }
    for (CheckAttribute check : checks) {
      sql.render(sql.commands().dropCheck(table, check)).end();
    }
    if (keyed) {
      dropPrimaryKey(sql);
    }
    if (from.attribute(DefaultAttribute.class).isPresent()) {
      sql.render(sql.commands().dropDefault(table, column)).end();
    }
    if (from.nullable() && !to.nullable()) {
      sql.append("UPDATE ").table(table).append(" SET ").name(column).append(" = " + sql.dialect().fill(to)
          + " WHERE ").name(column).append(" IS NULL").end();
    }
    sql.append("ALTER TABLE ").table(table).append(" ALTER COLUMN ");
    new SqlServerColumnDefinitionCommand(after, to).renderType(sql, to);
    sql.append(to.nullable() ? " NULL" : " NOT NULL").end();
    if (keyed) {
      sql.append("ALTER TABLE ").table(table).append(" ADD CONSTRAINT ").name(primaryKey.name())
          .append(" PRIMARY KEY (").names(primaryKey.columns()).append(")").end();
    }
    to.attribute(DefaultAttribute.class)
        .ifPresent(value -> sql.render(sql.commands().setDefault(table, column, value)).end());
    for (CheckAttribute check : checks) {
      sql.render(sql.commands().addCheck(table, check)).end();
    }
    for (IndexAttribute index : indexes) {
      sql.render(sql.commands().addIndex(table, index)).end();
    }
    for (ForeignKey key : keys) {
      sql.render(sql.commands().addForeignKey(key.table(), key.key())).end();
    }
  }

  /** Drops the primary key constraint, whatever its name, as a rename of the table may have renamed it. */
  private void dropPrimaryKey(ScriptWriter sql) {
    String qualified = sql.dialect().qualified(table);
    String statement = SqlServerVariable.of(sql, "primary_key").name();
    sql.append("DECLARE " + statement + " nvarchar(max) = (SELECT "
        + sql.dialect().string("ALTER TABLE " + qualified + " DROP CONSTRAINT ") + " + QUOTENAME(name) FROM"
        + " sys.key_constraints WHERE type = 'PK' AND parent_object_id = OBJECT_ID(" + sql.dialect().string(qualified)
        + ")) IF " + statement + " IS NOT NULL EXEC(" + statement + ")").end();
  }

  /** Returns the foreign keys that use the column and that the migration keeps: of this table and of others. */
  private List<ForeignKey> keys(ScriptWriter sql, Table before) {
    List<ForeignKey> keys = new ArrayList<>();
    for (TableName name : sql.after().names()) {
      Table other = sql.tableAfter(name);
      List<ForeignKeyAttribute> kept = sql.before().tableById(other.id())
          .map(old -> old.attributes(ForeignKeyAttribute.class)).orElse(List.of());
      for (ForeignKeyAttribute key : other.attributes(ForeignKeyAttribute.class)) {
        boolean uses = name.equals(table) && key.columns().contains(column)
            || key.table().equals(table) && key.referencedColumns().contains(column);
        if (uses && kept.contains(key)) {
          keys.add(new ForeignKey(name, key));
        }
      }
    }
    return keys;
  }

  /**
   * A foreign key with its table.
   *
   * @param table the table that has the foreign key
   * @param key the foreign key
   */
  private record ForeignKey(TableName table, ForeignKeyAttribute key) {
  }
}
