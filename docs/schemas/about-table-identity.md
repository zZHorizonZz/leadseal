---
description: Leadseal matches tables and columns by a stable id, so a rename keeps the rows and a new id drops the old table or column.
content_type: concept
---

# About table identity

Each table and column has an id.
Leadseal compares two schemas by id, not by name.
A table or column that keeps its id but gets another name is renamed, and keeps its rows.
A table or column whose id changes is dropped, and a new one is created.

## Ids of protobuf messages

| Thing  | Id                                                             | Example                         |
| ------ | -------------------------------------------------------------- | ------------------------------- |
| Table  | The full message name without the version parts of its package | `shop.v1.Order` is `shop.Order` |
| Column | The field number                                               | `int64 id = 1` is `1`           |

A version part of a package is a part such as `v1`, `v2` or `v2beta1`.
So `shop.v1.Order` and `shop.v2.Order` have the same id, and a message that moves to a new version of its package keeps its table.

The `id` table option sets another id.
Set it when a message moves to another package, so that its table keeps its rows.

## Ids in Java

`Table.of` and `Column.of` take the id as their first argument.
Any text works, if it stays the same for the life of the table or column.

## What changes keep the rows

| Change                                          | Result                                              |
| ----------------------------------------------- | --------------------------------------------------- |
| Rename a message, with the same id              | The table is renamed                                |
| Move a message to `v2` of its package           | Nothing changes                                     |
| Set another `schema` option                     | The table moves to that database schema             |
| Rename a field, with the same number            | The column is renamed                               |
| Renumber a field                                | The old column is dropped and a new column is added |
| Move a message to another package, without `id` | The old table is dropped and a new table is created |

## What table identity is not

- It is not the table name.
  Two tables in different database schemas can have the same name.
- It is not stored in the database.
  The ids are in the snapshots of the migration directory.
- It is not reused.
  A dropped id that comes back is a new table or column.
