---
description: Every field of the leadseal (leadseal.v1.column) protobuf option, which sets how a field of a table message maps to its column.
content_type: reference
---

# Column options

A field of a table message is a column.
The `(leadseal.v1.column)` option sets its name, key, indexes and foreign key.
To set its type, refer to [Column type options](column-type-options.md).

## `(leadseal.v1.column)`

A field option of type `ColumnOptions`.
Each field of a table message is a column, unless `ignore` is set.

| Name          | Type                | Default                                                                     | Description                                                                              |
| ------------- | ------------------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `primary_key` | `bool`              | `false`                                                                     | Whether the column is part of the primary key. The key has these fields in field order.  |
| `name`        | `string`            | the field name                                                              | The column name.                                                                         |
| `ignore`      | `bool`              | `false`                                                                     | Whether the field has no column.                                                         |
| `nullable`    | `optional bool`     | `true` with presence, for a repeated field and for a map; `false` otherwise | Whether the column accepts `NULL`. Refer to [Column types](column-types.md).             |
| `unique`      | `bool`              | `false`                                                                     | Whether the column has a unique index.                                                   |
| `index`       | `bool`              | `false`                                                                     | Whether the column has an index.                                                         |
| `check`       | `string`            | none                                                                        | A check constraint on the column, such as `quantity >= 0`.                               |
| `foreign_key` | `ForeignKeyOptions` | none                                                                        | A foreign key from this column to a column of another table.                             |
| `type`        | `oneof`             | follows from the field type                                                 | The column type and its options. Refer to [Column type options](column-type-options.md). |

Every table needs at least one `primary_key` field.
A primary key column cannot accept `NULL`.

### `foreign_key`

| Name                    | Type     | Default                                      | Description                                                                |
| ----------------------- | -------- | -------------------------------------------- | -------------------------------------------------------------------------- |
| `foreign_key.message`   | `string` | none                                         | The full name of the referenced table message, such as `shop.v1.Customer`. |
| `foreign_key.field`     | `string` | the single primary key field of that message | The referenced field.                                                      |
| `foreign_key.on_delete` | `Action` | `ACTION_NO_ACTION`                           | What happens to the row when the referenced row is deleted.                |
| `foreign_key.on_update` | `Action` | `ACTION_NO_ACTION`                           | What happens to the row when the referenced key changes.                   |
| `foreign_key.name`      | `string` | `fk_<table>_<columns>`                       | The constraint name.                                                       |

The referenced message must be a table message in the files that protoc compiles.
When it has more than one primary key field, set `field`, or use `foreign_key` of the table.

### `Action`

| Name               | Description                                                 |
| ------------------ | ----------------------------------------------------------- |
| `ACTION_NO_ACTION` | The change fails.                                           |
| `ACTION_RESTRICT`  | The change fails. SQL Server writes it as `NO ACTION`.      |
| `ACTION_CASCADE`   | The row is deleted or updated too.                          |
| `ACTION_SET_NULL`  | The column is set to `NULL`. The column must accept `NULL`. |

`ACTION_UNSPECIFIED` is `ACTION_NO_ACTION`.
