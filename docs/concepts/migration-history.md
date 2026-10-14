# Migration history

A migration directory holds every migration that leadseal generated, and a snapshot of the schema after each one.
Leadseal reads this directory each time it runs, so it only needs your current table messages to find what changed.

## The directory

```txt
db/migration/
  20261014T083047_create_orders/
    migration.json
    ops.json
    postgresql/V20261014083047__create_orders.sql
    sqlite/V20261014083047__create_orders.sql
  20261014T083256_add_orders_total/
    migration.json
    ops.json
    postgresql/V20261014083256__add_orders_total.sql
    sqlite/V20261014083256__add_orders_total.sql
  snapshots/
    3fb5dee7cb41bf96/schema.json
    28349903027be432/schema.json
```

Each migration folder has these files:

| File                                    | Contents                                                                                             |
| --------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `migration.json`                        | The migration name, the hash of the schema before it, the hash after it, and its time                |
| `ops.json`                              | The operations of the migration, such as `create_table` or `rename_column`                           |
| `<dialect>/V<version>__<name>.sql`      | The SQL of one database, in the Flyway file name format                                              |
| `<dialect>/V<version>__<name>.sql.conf` | Only for a script that must run outside a transaction. Refer to [Dialects](../reference/dialects.md) |

The folder name is the time of the migration in UTC, followed by a name that leadseal builds from the operations.
The version in the SQL file name is the same time without the `T`.

A snapshot is the schema after a migration, as JSON. Its folder name is the schema hash: the first 16 hexadecimal
digits of the SHA-256 hash of the schema JSON.

## How leadseal generates a migration

1. Leadseal reads every `migration.json` and orders the migrations into one chain. The first migration starts from
   the empty schema, and each next migration starts from the schema that the previous one created.
2. For each migration and each dialect that has no SQL file yet, leadseal writes the SQL from `ops.json` and the
   snapshots. This is how you add a database to an existing project.
3. Leadseal hashes the current schema. If the hash is the same as the hash after the latest migration, leadseal stops.
4. Leadseal compares the latest snapshot with the current schema. If they have no differences, leadseal stops.
5. Leadseal writes the snapshot, the migration folder, `ops.json`, `migration.json` and one SQL file for each dialect.

Leadseal never changes a file that exists. If you edit a generated SQL file, leadseal keeps your edit.

## Tables and columns keep their identity

Each table and column has an id. For protobuf, the table id is the full message name, such as `shop.v1.Order`, and
the column id is the field number. When a table or column keeps its id but gets another name, the migration renames
it and keeps its rows. When the id changes, the migration drops the old table or column and creates a new one.

Renumbering a field, or moving a message to another package, drops its column or table. Keep field numbers and full
message names stable.

## Branches

Two branches can each add a migration on top of the same schema. When you merge them, the directory has two
migrations that start from the same schema, and leadseal stops with an error that names both migrations. Delete one of
them and its snapshot, and run leadseal again. Leadseal then generates the deleted changes again, on top of the other
migration.

If a database already ran the deleted migration, that database has the changes already. Flyway reports the deleted
migration as missing. Refer to [Apply migrations with Flyway](../how-to/apply-migrations-with-flyway.md).
