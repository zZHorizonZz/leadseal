---
description: A leadseal schema is the set of tables that your protobuf table messages, or your Java code, declare.
content_type: overview
---

# Schemas

A schema is the set of tables that leadseal makes migrations for.
You declare it as protobuf messages with the `(leadseal.v1.table)` option, or build it in Java.
Each table has columns, a primary key, and optional indexes, checks, foreign keys and comments.

## Features

### Tables as protobuf messages

A message option makes a message a table, and a field option sets how a field maps to its column.
Refer to [Table options](table-options.md), [Column options](column-options.md) and [Column type options](column-type-options.md).

### Column types for every database

Each column has a leadseal column type, and each dialect writes it as a database type.
Refer to [Column types](column-types.md).

### Stable identity

Tables and columns are matched by id, so a rename keeps the rows.
Refer to [About table identity](about-table-identity.md).

### Checked before any SQL

Leadseal checks the schema before it writes a migration, and names the message or field that breaks a rule.
Refer to [Troubleshooting schemas](troubleshooting-schemas.md).

## Related

- [Migrations](../migrations/README.md)
- [Dialects](../dialects/README.md)
