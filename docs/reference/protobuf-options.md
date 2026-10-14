# Protobuf options

Import `leadseal/v1/options.proto` to use the options. A message with the `(leadseal.v1.table)` option is a table.
Other messages are not.

```proto
import "google/protobuf/timestamp.proto";
import "leadseal/v1/options.proto";

// The lines of an order.
message OrderLine {
  option (leadseal.v1.table) = {
    schema: "sales"
    index: {fields: ["product", "line"], unique: true}
    check: {expression: "quantity > 0"}
  };

  int64 order_id = 1 [(leadseal.v1.column) = {
    primary_key: true
    foreign_key: {message: "shop.v1.Order", on_delete: ACTION_CASCADE}
  }];
  int32 line = 2 [(leadseal.v1.column).primary_key = true];
  // The product name, as the customer saw it.
  string product = 3 [(leadseal.v1.column).text = {length: 100, collation: {key: "postgresql" value: "C"}}];
  int32 quantity = 4 [(leadseal.v1.column).integer.default_value = 1];
  string price = 5 [(leadseal.v1.column).decimal = {precision: 12, scale: 2}];
  google.protobuf.Timestamp added = 6 [(leadseal.v1.column) = {nullable: false, timestamp: {default_now: true}}];
}
```

## Comments

The leading comment of a table message is the table comment, and the leading comment of a field is the column
comment. In the example, the table comment is `The lines of an order.` SQLite has no comments, so it ignores them.

## `(leadseal.v1.table)`

A message option of type `TableOptions`.

| Field         | Type                              | Description                                                                                                     |
| ------------- | --------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `name`        | `string`                          | The table name. The default is the message name in snake_case: `OrderLine` is `order_line`                      |
| `schema`      | `string`                          | The database schema. The default is the default schema of the connection. SQLite has no schemas                 |
| `index`       | `repeated IndexOptions`           | The indexes of the table                                                                                        |
| `check`       | `repeated CheckOptions`           | The check constraints of the table                                                                              |
| `foreign_key` | `repeated TableForeignKeyOptions` | Foreign keys with more than one column                                                                          |
| `id`          | `string`                          | The stable identity of the table. The default is the full message name without the version parts of its package |

A schema is a namespace of tables. Table names must be unique in their namespace. On MariaDB, a namespace is a
database.

The `id` identifies the table across versions of the schema. A message that keeps its id but gets another name keeps
its table and rows. By default, the id is the full message name without the version parts of its package, such as
`v1` or `v2beta1`: `shop.v1.Order` and `shop.v2.Order` are the same table.

### `IndexOptions`

| Field        | Type              | Description                                                                                         |
| ------------ | ----------------- | --------------------------------------------------------------------------------------------------- |
| `fields`     | `repeated string` | The names of the indexed fields, in index order                                                     |
| `unique`     | `bool`            | Whether the index rejects two rows with the same values                                             |
| `name`       | `string`          | The index name. The default is `ix_<table>_<columns>`, or `ux_<table>_<columns>` for a unique index |
| `descending` | `repeated string` | The indexed fields that the index sorts in descending order                                         |
| `where`      | `string`          | The SQL condition of a partial index, such as `deleted = false`. MariaDB has no partial indexes     |

### `CheckOptions`

| Field        | Type     | Description                                                                            |
| ------------ | -------- | -------------------------------------------------------------------------------------- |
| `expression` | `string` | The SQL condition that every row must meet, such as `quantity > 0`                     |
| `name`       | `string` | The constraint name. The default is `ck_<table>_<hash>`, with a hash of the expression |

Every database gets the expression as it is. Use SQL that all your databases understand.

### `TableForeignKeyOptions`

| Field        | Type              | Description                                                                            |
| ------------ | ----------------- | -------------------------------------------------------------------------------------- |
| `fields`     | `repeated string` | The fields of this table, in key order                                                 |
| `message`    | `string`          | The full name of the referenced table message, such as `shop.v1.Order`                 |
| `references` | `repeated string` | The referenced fields, in key order. The default is the primary key of that message    |
| `on_delete`  | `Action`          | What happens to the row when the referenced row is deleted                             |
| `on_update`  | `Action`          | What happens to the row when the referenced key changes                                |
| `name`       | `string`          | The constraint name. The default is `fk_<table>_<columns>`                             |

## `(leadseal.v1.column)`

A field option of type `ColumnOptions`. Each field of a table message is a column, unless `ignore` is set.

| Field         | Type                | Description                                                                                                     |
| ------------- | ------------------- | --------------------------------------------------------------------------------------------------------------- |
| `primary_key` | `bool`              | Whether the column is part of the primary key. The key has these fields in field order                          |
| `name`        | `string`            | The column name. The default is the field name                                                                  |
| `ignore`      | `bool`              | Whether the field has no column                                                                                 |
| `nullable`    | `optional bool`     | Whether the column accepts `NULL`. The default follows from the field. Refer to [Column types](column-types.md) |
| `unique`      | `bool`              | Whether the column has a unique index                                                                           |
| `index`       | `bool`              | Whether the column has an index                                                                                 |
| `check`       | `string`            | A check constraint on the column, such as `quantity >= 0`                                                       |
| `foreign_key` | `ForeignKeyOptions` | A foreign key to a column of another table                                                                      |
| `type`        | `oneof`             | The column type and its options, one of the fields in the next table                                            |

Every table needs at least one `primary_key` field. A primary key column cannot accept `NULL`.

### The `type` oneof

Set one of these fields to choose the column type and its options. Without one, the type follows from the field
type. A field can only have a type that holds all its values. Refer to [Column types](column-types.md).

| Field       | Type              | Options                                                                                          |
| ----------- | ----------------- | ------------------------------------------------------------------------------------------------ |
| `boolean`   | `BooleanColumn`   | `default_value` (`bool`)                                                                         |
| `integer`   | `IntegerColumn`   | `format` (`FORMAT_INT32` or `FORMAT_INT64`), `default_value` (`int64`), `identity`               |
| `number`    | `NumberColumn`    | `format` (`FORMAT_FLOAT` or `FORMAT_DOUBLE`), `default_value` (`double`)                         |
| `decimal`   | `DecimalColumn`   | `precision`, `scale`, `default_value` (`string`, such as `0.00`)                                 |
| `text`      | `TextColumn`      | `length`, `default_value` (`string`), `collation` (map from dialect name to collation)           |
| `bytes`     | `BytesColumn`     | `length`, `default_value` (`bytes`)                                                              |
| `uuid`      | `UuidColumn`      | `default_value` (`string`), `default_random`                                                     |
| `timestamp` | `TimestampColumn` | `default_value` (`google.protobuf.Timestamp`), `default_now`                                     |
| `date`      | `DateColumn`      | `default_value` (`string`, such as `2026-10-14`), `default_today`                                |
| `time`      | `TimeColumn`      | `default_value` (`string`, such as `07:30:00`)                                                   |
| `json`      | `JsonColumn`      | `default_value` (`string`, such as `{}`)                                                         |

The options:

- `default_value`: the value of the column in a new row that does not set it. For an enum field, `text` takes a value
  name and `integer` takes a value number.
- `default_random`, `default_now` and `default_today`: the database generates a random UUID, the current time or the
  current date in UTC for each new row. Set either one of these or `default_value`.
- `identity`: the database generates the values of the column, for a primary key of one field. SQL Server cannot add
  or remove identity on an existing column.
- `length`: the maximum length of a text or bytes column. MariaDB and SQL Server need a length for a text or bytes
  column in a key or an index.
- `precision` and `scale`: the number of digits of a decimal column, and the number of digits after the decimal
  point. The default scale is 0.
- `collation`: the collation of a text column for each database, such as `{key: "mariadb" value: "utf8mb4_bin"}`. A
  database without an entry uses its default collation.

### `ForeignKeyOptions`

| Field       | Type     | Description                                                                                   |
| ----------- | -------- | --------------------------------------------------------------------------------------------- |
| `message`   | `string` | The full name of the referenced table message, such as `shop.v1.Order`                        |
| `field`     | `string` | The referenced field. The default is the primary key field of the referenced message          |
| `on_delete` | `Action` | What happens to the row when the referenced row is deleted. The default is `ACTION_NO_ACTION` |
| `on_update` | `Action` | What happens to the row when the referenced key changes. The default is `ACTION_NO_ACTION`    |
| `name`      | `string` | The constraint name. The default is `fk_<table>_<columns>`                                    |

The referenced message must be a table message in the files that protoc compiles. If it has more than one primary
key field, set `field`, or use a `foreign_key` of the table.

| `Action`           | When the referenced row is deleted or its key changes     |
| ------------------ | --------------------------------------------------------- |
| `ACTION_NO_ACTION` | The change fails                                          |
| `ACTION_RESTRICT`  | The change fails. SQL Server uses `NO ACTION`             |
| `ACTION_CASCADE`   | The row is deleted or updated too                         |
| `ACTION_SET_NULL`  | The column is set to `NULL`. The column must allow `NULL` |

## Names

Leadseal generates the names of keys, indexes and checks from the table and column names:

| Object       | Name                                 |
| ------------ | ------------------------------------ |
| Primary key  | `pk_<table>`                         |
| Index        | `ix_<table>_<columns>`               |
| Unique index | `ux_<table>_<columns>`               |
| Foreign key  | `fk_<table>_<columns>`               |
| Check        | `ck_<table>_<hash>`                  |
| Default      | `df_<table>_<column>`, on SQL Server |

A name longer than 63 characters is shortened, and ends with a hash of the full name.
