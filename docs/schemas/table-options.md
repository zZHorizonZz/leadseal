---
description: Every field of the leadseal (leadseal.v1.table) protobuf option, which makes a message a table.
content_type: reference
---

# Table options

Import `leadseal/v1/options.proto` to use the options.
A message with the `(leadseal.v1.table)` option is a table, and other messages are not.
Refer to [Generate migrations with protoc](../migrations/generate-migrations-with-protoc.md).

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

The leading comment of a table message is the table comment.
The leading comment of a field is the column comment.
In the example, the table comment is `The lines of an order.`
SQLite has no comments, so its migrations leave them out.

## `(leadseal.v1.table)`

A message option of type `TableOptions`.

| Name          | Type                              | Default                                                        | Description                                                                                          |
| ------------- | --------------------------------- | -------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `name`        | `string`                          | the message name in snake_case                                 | The table name. `OrderLine` is `order_line`.                                                         |
| `schema`      | `string`                          | the default schema of the connection                           | The database schema of the table. Table names must be unique within a schema. SQLite has no schemas. |
| `index`       | `repeated IndexOptions`           | none                                                           | The indexes of the table.                                                                            |
| `check`       | `repeated CheckOptions`           | none                                                           | The check constraints of the table.                                                                  |
| `foreign_key` | `repeated TableForeignKeyOptions` | none                                                           | The foreign keys with more than one column.                                                          |
| `id`          | `string`                          | the full message name without the version parts of its package | The stable identity of the table. Refer to [About table identity](about-table-identity.md).          |
| `trigger`     | `repeated TriggerOptions`         | none                                                           | The triggers of the table, in the order they run. Refer to [About triggers](about-triggers.md).      |

On MariaDB, a database schema is a database.

### `index[]`

| Name                 | Type              | Default                                                       | Description                                                                                                                       |
| -------------------- | ----------------- | ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `index[].fields`     | `repeated string` | none                                                          | The names of the indexed fields, in index order.                                                                                  |
| `index[].unique`     | `bool`            | `false`                                                       | Whether the index rejects two rows with the same values.                                                                          |
| `index[].name`       | `string`          | `ix_<table>_<columns>`, or `ux_<table>_<columns>` when unique | The index name.                                                                                                                   |
| `index[].descending` | `repeated string` | none                                                          | The indexed fields that the index sorts in descending order.                                                                      |
| `index[].where`      | `string`          | none                                                          | The SQL condition of a partial index, such as `deleted = false`. Every database gets it as it is. MariaDB has no partial indexes. |

### `check[]`

| Name                 | Type     | Default                                            | Description                                                                                           |
| -------------------- | -------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `check[].expression` | `string` | none                                               | The SQL condition that every row must meet, such as `quantity >= 0`. Every database gets it as it is. |
| `check[].name`       | `string` | `ck_<table>_<hash>`, with a hash of the expression | The constraint name.                                                                                  |

Use SQL that all your databases understand.

### `foreign_key[]`

| Name                       | Type              | Default                                | Description                                                             |
| -------------------------- | ----------------- | -------------------------------------- | ----------------------------------------------------------------------- |
| `foreign_key[].fields`     | `repeated string` | none                                   | The fields of this table, in key order.                                 |
| `foreign_key[].message`    | `string`          | none                                   | The full name of the referenced table message, such as `shop.v1.Order`. |
| `foreign_key[].references` | `repeated string` | the primary key fields of that message | The referenced fields, in key order.                                    |
| `foreign_key[].on_delete`  | `Action`          | `ACTION_NO_ACTION`                     | What happens to the row when the referenced row is deleted.             |
| `foreign_key[].on_update`  | `Action`          | `ACTION_NO_ACTION`                     | What happens to the row when the referenced key changes.                |
| `foreign_key[].name`       | `string`          | `fk_<table>_<columns>`                 | The constraint name.                                                    |

### `trigger[]`

| Name                | Type                     | Default            | Description                                                                                       |
| ------------------- | ------------------------ | ------------------ | ------------------------------------------------------------------------------------------------- |
| `trigger[].name`    | `string`                 | none, required     | The trigger name, unique in its table.                                                            |
| `trigger[].on`      | `repeated Event`         | none, at least one | The mutations that run the trigger.                                                               |
| `trigger[].when`    | `string`                 | every row          | The condition that a row must meet to run the actions, as an expression such as `changed(state)`. |
| `trigger[].actions` | `repeated ActionOptions` | none, at least one | The actions, in the order they run. Each has one of `insert`, `update`, `delete` and `assert`.    |

### `trigger[].actions[]`

| Name                | Type                  | Default        | Description                                                                                     |
| ------------------- | --------------------- | -------------- | ----------------------------------------------------------------------------------------------- |
| `insert.message`    | `string`              | none           | The full name of the table message to write a row into, such as `shop.v1.OrderEvent`.           |
| `insert.values`     | `map<string, string>` | none           | The value of each field of that row, by field name, as an expression such as `new.id`.          |
| `update.message`    | `string`              | none           | The full name of the table message whose rows change, such as `shop.v1.Customer`.               |
| `update.where`      | `string`              | none, required | The condition of the rows that change, such as `id == new.customer_id`.                         |
| `update.set`        | `map<string, string>` | none           | The new value of each field that changes, by field name, as an expression such as `orders + 1`. |
| `delete.message`    | `string`              | none           | The full name of the table message whose rows are deleted, such as `shop.v1.OrderEvent`.        |
| `delete.where`      | `string`              | none, required | The condition of the rows that are deleted, such as `order_id == old.id`.                       |
| `assert.expression` | `string`              | none           | The condition that every row must meet, as an expression such as `old.state != 'PAID'`.         |
| `assert.message`    | `string`              | none           | The message of the error when the condition is false.                                           |

In `update` and `delete`, a field name reads the other table, and `new.<field>` and `old.<field>` read the row of the trigger.

### `Event`

| Name           | Description         |
| -------------- | ------------------- |
| `EVENT_INSERT` | An insert of a row. |
| `EVENT_UPDATE` | An update of a row. |
| `EVENT_DELETE` | A delete of a row.  |

## Generated names

| Object                                             | Name                    |
| -------------------------------------------------- | ----------------------- |
| Primary key                                        | `pk_<table>`            |
| Index                                              | `ix_<table>_<columns>`  |
| Unique index                                       | `ux_<table>_<columns>`  |
| Foreign key                                        | `fk_<table>_<columns>`  |
| Check                                              | `ck_<table>_<hash>`     |
| Enum check                                         | `ck_<table>_<column>`   |
| Enum type, on PostgreSQL                           | `<enum>`                |
| Default, on SQL Server                             | `df_<table>_<column>`   |
| Trigger of a mutation                              | `tg_<table>_<mutation>` |
| Trigger of the rewrites, on PostgreSQL and MariaDB | `rw_<table>`            |

A name longer than 63 characters is shortened, and ends with a hash of the full name.
