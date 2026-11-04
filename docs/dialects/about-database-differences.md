---
description: How leadseal migrations work around what PostgreSQL, MariaDB, SQL Server and SQLite cannot change in place.
content_type: concept
---

# About database differences

Each dialect writes the same changes as different SQL.
Most differences are only syntax.
Some databases cannot make a change in place, so the migration takes more steps for them.

## PostgreSQL

A migration renames the primary key constraint together with its table, so the key keeps the name `pk_<table>`.
When a column becomes an identity column, the migration starts the identity after the largest existing value.

## MariaDB

MariaDB names every primary key `PRIMARY`.
A migration that renames a table does not rename its primary key.

A migration changes a column with `MODIFY COLUMN`, which writes the whole column definition.
The definition has the type, the default, the identity, the collation and the comment of the column.
Two changes of one column in one migration give one `MODIFY COLUMN`.

A database schema is a MariaDB database.
A migration that creates a table in a schema creates the database when it does not exist.

## SQL Server

SQL Server cannot change a column while an index, a key, a check or a default uses it.
A migration that changes such a column drops these objects first, and creates them again after the change.
It also drops and creates the foreign keys of other tables that reference the column.

Defaults are named constraints, `df_<table>_<column>`.
Renames do not rename them, so a migration that drops a default looks up its name.
A new column that does not accept `NULL` and has no default gets a temporary default that fills the existing rows.

SQL Server has no `RESTRICT` action.
A foreign key with `ACTION_RESTRICT` uses `NO ACTION`, which also rejects the change.
SQL Server cannot add `IDENTITY` to an existing column or remove it.

## SQLite

SQLite cannot change a column, a primary key, a foreign key or a check of an existing table.
A migration with such a change rebuilds the table, as the [SQLite documentation](https://www.sqlite.org/lang_altertable.html#otheralter) describes.
The rebuild creates the new table under a temporary name, copies the rows by column id, drops the old table, renames the new one, and creates its indexes.

A migration also rebuilds a table when it drops a column of the primary key or of a foreign key.
It rebuilds a table to add a column with a generated default too, because `ADD COLUMN` takes only constant defaults.

Dropping a table deletes its rows first.
When a foreign key of another table references the rebuilt table, that delete would run the `ON DELETE` action of the foreign key.
So a migration that rebuilds a referenced table turns off foreign keys while it runs:

```sql
PRAGMA foreign_keys = OFF;

CREATE TEMP TABLE "leadseal_check" (foreign_keys INTEGER CONSTRAINT foreign_keys_must_be_off CHECK (foreign_keys = 0), violations INTEGER CONSTRAINT foreign_keys_must_hold CHECK (violations = 0));
INSERT INTO "leadseal_check" (foreign_keys) SELECT foreign_keys FROM pragma_foreign_keys;

INSERT INTO "leadseal_check" (violations) SELECT COUNT(*) FROM pragma_foreign_key_check;
DROP TABLE "leadseal_check";

PRAGMA foreign_keys = ON;
```

The rebuild runs between the two `INSERT` statements.
The first `INSERT` fails if foreign keys are still on, and the second fails if a row breaks a foreign key.

SQLite ignores `PRAGMA foreign_keys` inside a transaction, so this script must run outside one.
Leadseal writes a `.sql.conf` file with `executeInTransaction=false` next to the script, and Flyway reads it.
If the script runs in a transaction while foreign keys are on, it fails before it changes a table.
Outside a transaction, SQLite runs each statement on its own, so the statements before a failure stay applied.

An `INT` or `BIGINT` primary key of one column is the rowid of the table.
SQLite generates its values with or without identity.
SQLite cannot drop a default, so a new column that does not accept `NULL` and has no default keeps its zero value as its default.
SQLite cannot rename a constraint, so a renamed table keeps the name of its primary key.

## What a dialect is not

- It is not a connection.
  A dialect writes SQL, and never connects to a database.
- It is not a migration tool.
  Flyway, or another tool, runs the SQL.
- It is not lossy.
  A dialect that cannot make a change stops with an error, and does not write a partial migration.
