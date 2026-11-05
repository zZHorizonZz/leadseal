---
description: Leadseal drops the tables and columns that you remove and warns about them, and refuses a type change that can lose values.
content_type: concept
---

# About data loss

Some changes delete data.
Leadseal applies a change that deletes data your schema no longer has, and warns about it.
It refuses a change that can lose values your schema still has, unless you allow unsafe changes.

## Drops

When you remove a field, its column is dropped with its values.
When you remove a table message, or its table option, the table is dropped with its rows.
Removing them is the decision, so leadseal applies the drop.

Each drop also gives a warning:

- The plugin prints a line for each drop, such as `leadseal: The migration 20261014070000 drops the column orders.note and its values.`
- The SQL file of each database starts with a comment for each drop, such as `-- Warning: drops the column orders.note and its values.`

Review the warnings before you commit the migration.
A drop that you did not mean usually comes from a changed id:

| Change                                          | Result                                              | To keep the data                    |
| ----------------------------------------------- | --------------------------------------------------- | ----------------------------------- |
| Renumber a field                                | The old column is dropped and a new column is added | Keep the field number               |
| Move a message to another package, without `id` | The old table is dropped and a new table is created | Set the old id with the `id` option |
| Leave a file out of the protoc inputs           | Each table of the file is dropped                   | Add the file to the protoc inputs   |

Refer to [About table identity](../schemas/about-table-identity.md).

## Unsafe changes

A change is unsafe when it can lose values that your schema still has.
That is a column type change to a type that holds fewer values: a shorter, smaller or less precise type, or another kind of type.
Examples are `text` to `text(20)`, a 64-bit integer to a 32-bit one, `decimal(12, 2)` to `decimal(12, 1)`, and text to an integer.
Removing a value of an enum also gives the column a type that holds fewer values.

Leadseal refuses an unsafe change, because the schema cannot tell what to do with a value that the new type cannot hold.
A change to a type that holds every value, such as `text(20)` to `text`, is safe.

The plugin option `allow_unsafe=true` applies unsafe changes, with a warning for each one.
From Java, `MigrationOptions.allowUnsafe()` does the same.
Use it for one build, after you check the rows, or while you prototype a schema that no shared database runs yet.

## Gel

Leadseal follows the migrations of [Gel](https://docs.geldata.com/reference/cli/gel_migration/gel_migration_create), with one difference.
Gel compares schemas by name, so it asks at a prompt whether a missing property is dropped or renamed.
Leadseal compares tables and columns by id, so a removed field is a drop and a renamed field is a rename, with no question.
Like `gel migration create --non-interactive`, leadseal refuses unsafe changes, and like `--allow-unsafe`, `allow_unsafe` applies them.

## Related

- [Protoc plugin options](protoc-plugin-options.md)
- [About migration history](about-migration-history.md)
- [About table identity](../schemas/about-table-identity.md)
