---
description: A leadseal dialect writes the changes of a migration as the SQL of one database: PostgreSQL, MariaDB, SQL Server or SQLite.
content_type: overview
---

# Dialects

A dialect writes the changes of a migration as the SQL of one database.
Each dialect is its own module, and registers itself for `ServiceLoader` under its name.
The name is also the name of the folder with the SQL files of the dialect.

| Dialect            | Name         | Module                       |
| ------------------ | ------------ | ---------------------------- |
| `PostgresDialect`  | `postgresql` | `leadseal-dialect-postgres`  |
| `MariaDbDialect`   | `mariadb`    | `leadseal-dialect-mariadb`   |
| `SqlServerDialect` | `sqlserver`  | `leadseal-dialect-sqlserver` |
| `SqliteDialect`    | `sqlite`     | `leadseal-dialect-sqlite`    |

## Features

### One schema, four databases

Each migration has the SQL of every dialect that you choose, from the same changes.
Refer to [Dialect support](dialect-support.md).

### Changes that a database cannot make in place

SQLite rebuilds a table, and SQL Server drops and creates the objects that use a column.
Refer to [About database differences](about-database-differences.md).

### Errors before any SQL runs

When a database cannot make a change, leadseal stops with an `UnsupportedChangeException` and writes nothing.
Refer to [Troubleshooting dialects](troubleshooting-dialects.md).

### Your own dialect

A dialect is a Java class and a `Ddl`, tested with the contract tests of `leadseal-dialect`.
Refer to [Write a dialect](write-a-dialect.md).

## Related

- [Column types](../schemas/column-types.md)
- [Migrations](../migrations/README.md)
