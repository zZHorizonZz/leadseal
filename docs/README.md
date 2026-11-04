---
description: Leadseal generates SQL migration files for PostgreSQL, MariaDB, SQL Server and SQLite from protobuf table messages.
content_type: overview
---

# Leadseal

Leadseal generates SQL migration files from protobuf messages.
You mark a message as a table with a protobuf option.
When protoc runs, leadseal compares your table messages with the schema of the latest migration.
It then writes a new migration with the SQL of each database that you choose.
You apply the migrations with Flyway or another migration tool.

## Features

### Migrations from protoc

`protoc-gen-leadseal` runs in your build and writes a migration each time a table message changes.
When nothing changes, it writes nothing.
Refer to [Generate migrations with protoc](migrations/generate-migrations-with-protoc.md).

### Migrations from Java

You can build a schema in Java and generate the same migrations without protoc.
Refer to [Generate migrations from Java](migrations/generate-migrations-from-java.md).

### A history in your repository

The migration directory holds every migration and a snapshot of the schema after each one.
You commit it with your code, and leadseal reads it on every run.
Refer to [About migration history](migrations/about-migration-history.md).

### Tables that keep their rows

Each table and column has a stable id.
A rename keeps the rows, and a field number change drops the column.
Refer to [About table identity](schemas/about-table-identity.md).

### Four databases

One schema gives the SQL of PostgreSQL, MariaDB, SQL Server and SQLite.
Leadseal stops with an error when a database cannot make a change.
Refer to [Dialect support](dialects/dialect-support.md).

## Related

- [Get started](get-started.md)
- [Migrations](migrations/README.md)
- [Schemas](schemas/README.md)
- [Dialects](dialects/README.md)
