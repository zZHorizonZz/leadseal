# Protobuf options

Import `leadseal/v1/options.proto` to use the options. A message with the `(leadseal.v1.table)` option is a table.
Other messages are not.

```proto
import "leadseal/v1/options.proto";

message OrderLine {
  option (leadseal.v1.table) = {
    index: {fields: ["product", "line"], unique: true}
  };

  int64 order_id = 1 [(leadseal.v1.column) = {
    primary_key: true
    foreign_key: {message: "shop.v1.Order", on_delete: ACTION_CASCADE}
  }];
  int32 line = 2 [(leadseal.v1.column).primary_key = true];
  string product = 3 [(leadseal.v1.column).length = 100];
}
```

## `(leadseal.v1.table)`

A message option of type `TableOptions`.

| Field   | Type                    | Description                                                                                |
| ------- | ----------------------- | ------------------------------------------------------------------------------------------ |
| `name`  | `string`                | The table name. The default is the message name in snake_case: `OrderLine` is `order_line` |
| `index` | `repeated IndexOptions` | The indexes of the table                                                                   |

### `IndexOptions`

| Field    | Type              | Description                                                                                         |
| -------- | ----------------- | --------------------------------------------------------------------------------------------------- |
| `fields` | `repeated string` | The names of the indexed fields, in index order                                                     |
| `unique` | `bool`            | Whether the index rejects two rows with the same values                                             |
| `name`   | `string`          | The index name. The default is `ix_<table>_<columns>`, or `ux_<table>_<columns>` for a unique index |

## `(leadseal.v1.column)`

A field option of type `ColumnOptions`. Each field of a table message is a column, unless `ignore` is set.

| Field         | Type                | Description                                                                            |
| ------------- | ------------------- | -------------------------------------------------------------------------------------- |
| `primary_key` | `bool`              | Whether the column is part of the primary key. The key has these fields in field order |
| `name`        | `string`            | The column name. The default is the field name                                         |
| `type`        | `Type`              | Another column type than the default. Refer to [Column types](column-types.md)         |
| `length`      | `uint32`            | The maximum length of a text or bytes column                                           |
| `foreign_key` | `ForeignKeyOptions` | A foreign key to a column of another table                                             |
| `ignore`      | `bool`              | Whether the field has no column                                                        |
| `unique`      | `bool`              | Whether the column has a unique index                                                  |
| `index`       | `bool`              | Whether the column has an index                                                        |

Every table needs at least one `primary_key` field. A primary key field must not have presence, so it cannot be
`optional` or a message field.

### `ForeignKeyOptions`

| Field       | Type     | Description                                                                                   |
| ----------- | -------- | --------------------------------------------------------------------------------------------- |
| `message`   | `string` | The full name of the referenced table message, such as `shop.v1.Order`                        |
| `field`     | `string` | The referenced field. The default is the primary key field of the referenced message          |
| `on_delete` | `Action` | What happens to the row when the referenced row is deleted. The default is `ACTION_NO_ACTION` |

The referenced message must be a table message in the files that protoc compiles. If it has more than one primary
key field, set `field`.

| `Action`           | When the referenced row is deleted                        |
| ------------------ | --------------------------------------------------------- |
| `ACTION_NO_ACTION` | The delete fails                                          |
| `ACTION_RESTRICT`  | The delete fails. SQL Server uses `NO ACTION`             |
| `ACTION_CASCADE`   | The row is deleted too                                    |
| `ACTION_SET_NULL`  | The column is set to `NULL`. The column must allow `NULL` |

## Names

Leadseal generates the names of keys and indexes from the table and column names:

| Object       | Name                                    |
| ------------ | --------------------------------------- |
| Primary key  | `pk_<table>`                            |
| Index        | `ix_<table>_<columns>`                  |
| Unique index | `ux_<table>_<columns>`                  |
| Foreign key  | `fk_<table>_<columns>`                  |

A name longer than 63 characters is shortened, and ends with a hash of the full name.
