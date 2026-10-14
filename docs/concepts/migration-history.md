# Migration history

A migration directory holds every migration that leadseal generated, and a snapshot of the schema after each one.
Leadseal reads this directory each time it runs, so it only needs your current table messages to find what changed.

## The directory

```txt
db/migration/
  20261014104830/
    migration.json
    postgresql/V20261014104830.sql
    sqlite/V20261014104830.sql
  20261014104833/
    migration.json
    postgresql/V20261014104833.sql
    sqlite/V20261014104833.sql
  snapshots/
    298c2267bb456adc/contract.json
    e1ef0ebd6b8b8350/contract.json
```

Each migration folder has these files:

| File                                    | Contents                                                                                              |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `migration.json`                        | The version, the name, the schema hashes before and after, the time, and the changes of the migration |
| `<dialect>/V<version>.sql`              | The SQL of one database, in the Flyway file name format                                               |
| `<dialect>/V<version>.sql.conf`         | Only for a script that must run outside a transaction. Refer to [Dialects](../reference/dialects.md)  |

The folder name is the version of the migration, followed by a name that leadseal builds from the changes. The version
is the time of the migration in UTC, such as `20261014104830`, and the SQL file names use it as their Flyway version.
Leadseal stops with an error if a folder name does not match the version and name in its `migration.json`.

A snapshot is the schema after a migration, in a `contract.json` file. It has the version of the file format in
`schemaVersion`, the tables of each namespace in `storage`, and their hash in `storageHash`. The folder name of a
snapshot is the same hash: the first 16 hexadecimal digits of the SHA-256 hash of the storage JSON.

## How leadseal generates a migration

1. Leadseal reads every `migration.json` and orders the migrations by version. The first migration must start from
   the empty schema, and each next migration from the schema that the previous one created.
2. For each migration and each dialect that has no SQL file yet, leadseal writes the SQL from the changes in
   `migration.json` and the snapshots. This is how you add a database to an existing project.
3. Leadseal hashes the current schema. If the hash is the same as the hash after the latest migration, leadseal stops.
4. Leadseal compares the latest snapshot with the current schema. If they have no differences, leadseal stops.
5. Leadseal writes the snapshot, the migration folder, `migration.json` and one SQL file for each dialect.

Leadseal never changes a file that exists. If you edit a generated SQL file, leadseal keeps your edit.

## Tables and columns keep their identity

Each table and column has an id. For protobuf, the table id is the full message name without the version parts of its
package, and the column id is the field number. Both `shop.v1.Order` and `shop.v2.Order` have the id `shop.Order`, so
moving a message to a new version of its package keeps its table. When a table or column keeps its id but gets another
name or namespace, the migration renames or moves it and keeps its rows. When the id changes, the migration drops the
old table or column and creates a new one.

Renumbering a field, or moving a message to another package, drops its column or table. Keep field numbers stable, and
set the `id` table option when a message moves to another package.

## Branches

Two branches can each add a migration on top of the same schema. When you merge them, the directory has two
migrations that start from the same schema, and leadseal stops with an error that names both migrations. Delete the
later one and its snapshot, and run leadseal again. Leadseal then generates the deleted changes again, on top of the
other migration.

If a database already ran the deleted migration, that database has the changes already. Flyway reports the deleted
migration as missing. Refer to [Apply migrations with Flyway](../how-to/apply-migrations-with-flyway.md).
