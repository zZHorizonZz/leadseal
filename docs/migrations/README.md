---
description: Leadseal migrations are SQL files for each database, generated from a migration directory that you commit with your code.
content_type: overview
---

# Migrations

A migration is a folder with the SQL of each database that you choose, and a `migration.json` that records its changes.
Leadseal writes a new migration when your schema differs from the schema after the latest migration.
You generate migrations with protoc or from Java, and apply them with Flyway or another migration tool.

## Features

### Generate with protoc

`protoc-gen-leadseal` runs in your build, reads the migration directory and writes the next migration.
Refer to [Generate migrations with protoc](generate-migrations-with-protoc.md).

### Generate from Java

`MigrationDirectory` plans and writes the same files from a `Schema` that you build in Java.
Refer to [Generate migrations from Java](generate-migrations-from-java.md).

### Apply with Flyway

The SQL file names follow the Flyway format, and one wildcard location finds the files of one database.
Refer to [Apply migrations with Flyway](apply-migrations-with-flyway.md).

### Warnings about data loss

A removed field or message drops its column or table, and the build warns about each drop.
A type change that can lose values stops the build, unless you allow unsafe changes.
Refer to [About data loss](about-data-loss.md).

### Merge branches

Two branches that each add a migration give two migrations on top of the same schema.
Refer to [Merge migration branches](merge-migration-branches.md).

### Errors that name who fixes them

Each exception says whether the schema, a database, the history or the build has to change.
Refer to [Exceptions](exceptions.md) and [Troubleshooting migrations](troubleshooting-migrations.md).

## Related

- [About migration history](about-migration-history.md)
- [Protoc plugin options](protoc-plugin-options.md)
- [Schemas](../schemas/README.md)
- [Dialects](../dialects/README.md)
