# Dialects

A dialect writes the operations of a migration as the SQL of one database. Each dialect is in its own module, and
registers itself for `ServiceLoader` under its name.

| Dialect            | Name         | Module                       |
| ------------------ | ------------ | ---------------------------- |
| `PostgresDialect`  | `postgresql` | `leadseal-dialect-postgres`  |
| `MariaDbDialect`   | `mariadb`    | `leadseal-dialect-mariadb`   |
| `SqlServerDialect` | `sqlserver`  | `leadseal-dialect-sqlserver` |
| `SqliteDialect`    | `sqlite`     | `leadseal-dialect-sqlite`    |

The name is also the name of the folder with the SQL files of the dialect.

## Operations

| Operation            | PostgreSQL                          | MariaDB                             | SQL Server                          | SQLite                           |
| -------------------- | ----------------------------------- | ----------------------------------- | ----------------------------------- | -------------------------------- |
| Create a table       | `CREATE TABLE`                      | `CREATE TABLE`                      | `CREATE TABLE`                      | `CREATE TABLE` with foreign keys |
| Rename a table       | `RENAME TO`, `RENAME CONSTRAINT`    | `RENAME TO`                         | `sp_rename`                         | `RENAME TO`                      |
| Add a column         | `ADD COLUMN`                        | `ADD COLUMN`                        | `ADD`                               | `ADD COLUMN`                     |
| Rename a column      | `RENAME COLUMN`                     | `RENAME COLUMN`                     | `sp_rename`                         | `RENAME COLUMN`                  |
| Change a column      | `ALTER COLUMN ... TYPE ... USING`   | `MODIFY COLUMN`                     | `ALTER COLUMN`                      | Rebuild                          |
| Change a primary key | `DROP CONSTRAINT`, `ADD CONSTRAINT` | `DROP PRIMARY KEY, ADD PRIMARY KEY` | `DROP CONSTRAINT`, `ADD CONSTRAINT` | Rebuild                          |
| Drop an index        | `DROP INDEX`                        | `DROP INDEX ... ON`                 | `DROP INDEX ... ON`                 | `DROP INDEX`                     |
| Add a foreign key    | `ADD CONSTRAINT`                    | `ADD CONSTRAINT`                    | `ADD CONSTRAINT`                    | Rebuild                          |
| Drop a foreign key   | `DROP CONSTRAINT`                   | `DROP FOREIGN KEY`                  | `DROP CONSTRAINT`                   | Rebuild                          |

## PostgreSQL

A migration renames the primary key constraint together with its table, so the key keeps the name `pk_<table>`.

## MariaDB

MariaDB names every primary key `PRIMARY`. A migration that renames a table does not rename its primary key.

## SQL Server

SQL Server cannot change a column while an index, the primary key or a foreign key of its table uses the column. A
migration that changes such a column drops these objects first and creates them again after the change.

A new column that does not accept `NULL` gets a default constraint that fills the existing rows. The migration drops
the constraint after it adds the column.

SQL Server has no `ON DELETE RESTRICT`. A foreign key with `ACTION_RESTRICT` uses `NO ACTION`, which also rejects the
delete.

## SQLite

SQLite cannot change a column, a primary key or a foreign key of an existing table. A migration with such a change
rebuilds the table, as the [SQLite documentation](https://www.sqlite.org/lang_altertable.html#otheralter) describes:

1. Create the new table under a temporary name.
2. Copy the rows. Columns are matched by id.
3. Drop the old table.
4. Rename the new table, and create its indexes.

A migration also rebuilds a table when it drops a column that is part of the primary key or a foreign key.

Dropping a table deletes its rows first. When a foreign key of another table references the rebuilt table, this
delete would run the `ON DELETE` action of the foreign key, and could delete or change the rows of the other table. So
a migration that rebuilds a referenced table turns off foreign keys while it runs:

```sql
PRAGMA foreign_keys = OFF;

-- The migration fails here if foreign keys are still on.
CREATE TEMP TABLE leadseal_check (...);
INSERT INTO leadseal_check (foreign_keys) SELECT foreign_keys FROM pragma_foreign_keys;

-- The rebuild.

-- The migration fails here if a row breaks a foreign key.
INSERT INTO leadseal_check (violations) SELECT COUNT(*) FROM pragma_foreign_key_check;
DROP TABLE leadseal_check;

PRAGMA foreign_keys = ON;
```

SQLite ignores `PRAGMA foreign_keys` inside a transaction, so the script must run outside one. Leadseal writes a
`.sql.conf` file with `executeInTransaction=false` next to the script, which tells Flyway to run it outside a
transaction. If the script runs in a transaction while foreign keys are on, it fails before it changes a table.

Outside a transaction, SQLite runs each statement of the script on its own. If a statement fails, the statements
before it stay applied. The script turns foreign keys on at the end, also on a connection that had them off.

SQLite cannot drop a default value, so a new column that does not accept `NULL` keeps its zero value as default.
SQLite cannot rename a constraint, so a renamed table keeps the name of its primary key.
