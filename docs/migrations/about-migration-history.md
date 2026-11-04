---
description: The leadseal migration directory holds every migration and a snapshot of the schema after each one, so leadseal needs only your current tables.
content_type: concept
---

# About migration history

A migration directory holds every migration that leadseal generated, and a snapshot of the schema after each one.
Leadseal reads this directory on every run.
It then needs only your current tables to find what changed.

## The directory

```text
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

| File | Contents |
| --- | --- |
| `<version>/migration.json` | The version, the name, the schema hashes before and after, the time, and the changes of the migration |
| `<version>/<dialect>/V<version>.sql` | The SQL of one database, in the Flyway file name format |
| `<version>/<dialect>/V<version>.sql.conf` | `executeInTransaction=false`, only for a script that must run outside a transaction |
| `snapshots/<hash>/contract.json` | The schema after a migration |

The version is the time of the migration in UTC, such as `20261014104830`.
The folder name is the version, and the SQL file names use it as their Flyway version.
The name in `migration.json` describes the changes, such as `create_orders` or `add_orders_note_and_2_more`.

A snapshot has the version of its file format in `schemaVersion`, the tables of each database schema in `storage`, and their hash in `storageHash`.
The folder name of a snapshot is that hash: the first 16 hexadecimal digits of the SHA-256 hash of the storage JSON.

## How a migration follows the one before it

Each migration records the hash of the schema it starts from and the hash of the schema it creates.
The first migration starts from the empty schema.
Each later migration starts from the schema that the migration before it creates.
The migrations form one chain, ordered by version.

On each run, leadseal does this:

| Stage | What leadseal does |
| --- | --- |
| Read | It reads every `migration.json`, orders the migrations by version and checks the chain. |
| Fill in | It writes the SQL of each dialect that an existing migration has no file for. This is how you add a database to a project. |
| Compare | It hashes your schema. When the hash is the hash after the latest migration, it stops. |
| Diff | It compares the latest snapshot with your schema. When they have no differences, it stops. |
| Write | It writes the snapshot, the migration folder, `migration.json` and one SQL file for each dialect. |

Leadseal never changes a file that exists.
When you edit a generated SQL file, leadseal keeps your edit.

## Changes run in phases

The changes of one migration run phase by phase, in this order:

| Phase | Changes |
| --- | --- |
| 1 | Drop foreign keys, checks and indexes |
| 2 | Drop tables |
| 3 | Rename tables, then move tables to other database schemas |
| 4 | Create tables, without their foreign keys |
| 5 | Rename columns, then add columns |
| 6 | Drop column attributes, change column types, then add and change column attributes |
| 7 | Change primary keys |
| 8 | Drop columns |
| 9 | Add indexes, checks and foreign keys |
| 10 | Change table comments |

A foreign key is dropped before the tables and columns that it reads, and added after all of them exist.

## What the migration history is not

- It is not a record of the databases.
  Flyway records which migrations a database ran.
- It is not rewritten.
  Leadseal adds files and never edits or deletes one.
- It is not optional.
  Without the snapshots, leadseal cannot find what changed.
- It is not one file per database.
  One migration has the SQL of every dialect you choose.
