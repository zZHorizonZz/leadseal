---
description: A rewrite sets the value of a column on each update of a row, such as the time of the last change or a revision number.
content_type: concept
---

# About rewrites

A rewrite is a value that the database writes into a column on each update of a row, in place of the value that the update writes.
Use it for a value that follows from the row, such as the time of the last change, a revision number or a slug of a title.

```proto
message Post {
  option (leadseal.v1.table) = {};

  int64 id = 1 [(leadseal.v1.column).primary_key = true];
  string title = 2;
  string slug = 3 [(leadseal.v1.column).rewrite = "lower(title)"];
  int64 revision = 4 [(leadseal.v1.column) = {integer: {default_value: 0}, rewrite: "old.revision + 1"}];
  google.protobuf.Timestamp modified = 5 [(leadseal.v1.column) = {timestamp: {default_now: true}, rewrite: "now()"}];
}
```

After `UPDATE posts SET title = 'Hello'`, the row has the slug `hello`, a revision one larger than before, and the current time as `modified`.

## Inserts, updates and defaults

A rewrite runs on updates.
On an insert, a column gets the value that the insert writes, or its default when the insert writes none.
So a column with a rewrite takes its first value from its default, as `revision` and `modified` do in the example.
Refer to [Column type options](column-type-options.md) for defaults.

## What a rewrite reads

The value is an expression in a subset of [CEL](https://cel.dev), and has the type of the column.
It names fields by their protobuf names, not by their column names:

| Name                   | Value                                         |
| ---------------------- | --------------------------------------------- |
| `title` or `new.title` | The value of the field that the update writes |
| `old.title`            | The value of the field before the update      |

An expression can use:

- Constants: `1`, `1.5`, `'text'`, `true`, `b'\x00'` and `timestamp('2026-01-01T00:00:00Z')`.
- The operators `== != < <= > >=`, `&& || !`, `+ - * / %` on numbers, `+` on texts, and `value in [a, b]`.
- `condition ? a : b`, `has(field)`, and `field == null` on a field that can be `NULL`.
- The functions `now()`, `uuid()`, `lower`, `upper`, `trim`, `length`, `abs`, `coalesce`, and `changed(field)`, which is true when the update changes the field.

A divisor must be a constant that is not zero.
Fields of the types date, time and JSON cannot be read yet.
An expression also reads the row that a key of the table references, as in `author.name`.
Refer to [About triggers](about-triggers.md#what-a-trigger-reads).

A rewrite that reads its own field reads the value that the update writes.
For example, `changed(slug) ? slug : lower(title)` keeps a slug that the update sets, and otherwise follows the title.

## Rewrites that read other rewrites

A rewrite that reads a field with another rewrite reads the rewritten value.
With `label` rewritten as `upper(slug)` and `slug` as `lower(title)`, an update first writes the slug and then the label from it.
Leadseal runs the rewrites in that order, whatever the order of the fields.
Two rewrites that read each other, directly or through others, stop the build.

## How the database writes a rewrite

The database writes the rewrites of a table in a trigger that runs on each update of a row.

| Database   | Trigger                                                                                                                |
| ---------- | ---------------------------------------------------------------------------------------------------------------------- |
| PostgreSQL | `rw_<table>`, a `BEFORE UPDATE` trigger with a function of the same name, which sets the new row before it is written. |
| MariaDB    | `rw_<table>`, a `BEFORE UPDATE` trigger, which sets the new row before it is written.                                  |
| SQLite     | `tg_<table>_update`, an `AFTER UPDATE` trigger, which updates the row again and then runs the triggers of the update.  |
| SQL Server | `tg_<table>_update`, an `AFTER UPDATE` trigger, which updates the rows again and then runs the triggers of the update. |

On SQLite and SQL Server, the triggers of the update are in the same database trigger, so the second update of the row does not run them again.

A trigger reads its columns by their names.
A migration that renames a column that a rewrite reads, renames or moves the table, or changes its primary key, drops the trigger and creates it again.
Refer to [About database differences](../dialects/about-database-differences.md).

## Limits

- A rewrite cannot set a column of the primary key.
- A column has at most one rewrite.

## Related

- [About triggers](about-triggers.md)
- [Column options](column-options.md)
- [Column type options](column-type-options.md)
- [Troubleshooting schemas](troubleshooting-schemas.md)
