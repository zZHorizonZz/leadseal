---
description: The leadseal column type of each protobuf field type, and the database type that each dialect writes for it.
content_type: reference
---

# Column types

Each column has a leadseal column type, and each dialect writes it as a database type.
To choose another type for a field, refer to [Column type options](column-type-options.md).

## Field types

Without a `type` option, the column type follows from the field type:

| Field type                                                              | Column type                        | Accepts `NULL` |
| ----------------------------------------------------------------------- | ---------------------------------- | -------------- |
| `bool`                                                                  | `BOOLEAN`                          | with presence  |
| `int32`, `sint32`, `sfixed32`                                           | `INT`                              | with presence  |
| `uint32`, `fixed32`, `int64`, `uint64`, `sint64`, `fixed64`, `sfixed64` | `BIGINT`                           | with presence  |
| `float`                                                                 | `REAL`                             | with presence  |
| `double`                                                                | `DOUBLE`                           | with presence  |
| `string`                                                                | `TEXT`                             | with presence  |
| An enum                                                                 | `ENUM`, the value name             | with presence  |
| `bytes`                                                                 | `BYTES`                            | with presence  |
| `google.protobuf.Timestamp`                                             | `TIMESTAMP`                        | yes            |
| `google.type.Date`                                                      | `DATE`                             | yes            |
| `google.type.TimeOfDay`                                                 | `TIME`                             | yes            |
| `google.type.Decimal`                                                   | `DECIMAL`, with a `decimal` option | yes            |
| `google.protobuf.BoolValue`                                             | `BOOLEAN`                          | yes            |
| `google.protobuf.Int32Value`                                            | `INT`                              | yes            |
| `google.protobuf.UInt32Value`, `Int64Value`, `UInt64Value`              | `BIGINT`                           | yes            |
| `google.protobuf.FloatValue`                                            | `REAL`                             | yes            |
| `google.protobuf.DoubleValue`                                           | `DOUBLE`                           | yes            |
| `google.protobuf.StringValue`                                           | `TEXT`                             | yes            |
| `google.protobuf.BytesValue`                                            | `BYTES`                            | yes            |
| Another message                                                         | `JSON`                             | yes            |
| A repeated field or a map                                               | `JSON`                             | yes            |

A field has presence when it has the `optional` label, is a message field, or is in a `oneof`.
The `nullable` column option sets another value, such as `nullable: false` for a `Timestamp` that every row has.

## Other column types of a field

A field can only have a type that holds all its values:

| Field type       | Other column types                                     |
| ---------------- | ------------------------------------------------------ |
| `string`         | `uuid`, `json`, `decimal`, `date`, `time`, `timestamp` |
| `bytes`          | `uuid`                                                 |
| An enum          | `integer`, which stores the value number               |
| A 32-bit integer | `integer` with `FORMAT_INT64`                          |
| `float`          | `number` with `FORMAT_DOUBLE`                          |

## Enum columns

An enum column accepts only the values of its enum.
It stores the value names, or the value numbers with the `integer` type option.
An enum with aliases, that is `option allow_alias = true`, gives each number once, under its first name.

Each enum has a name: its path in its package, in snake case, such as `order_line_state` for `OrderLine.State`.
Two enums in a schema cannot have the same name and other values, and an enum cannot have the name of a table.

Each dialect writes an enum column its own way:

| Dialect    | Stores names                                       | Stores numbers                                |
| ---------- | -------------------------------------------------- | --------------------------------------------- |
| PostgreSQL | The enum type `<enum>`                             | `integer` and the check `ck_<table>_<column>` |
| MariaDB    | `ENUM('NEW', 'SENT')`                              | `INT` and the check `ck_<table>_<column>`     |
| SQL Server | `NVARCHAR(63)` and the check `ck_<table>_<column>` | `INT` and the check `ck_<table>_<column>`     |
| SQLite     | `TEXT` and the check `ck_<table>_<column>`         | `INTEGER` and the check `ck_<table>_<column>` |

On PostgreSQL, the columns that store the names of an enum share its type.
A migration creates the type before the first column that uses it, and drops it after the last column stops using it.
The names of an enum and of its values have at most 63 characters, which is the limit of PostgreSQL.

| Change of the enum                    | PostgreSQL                                                | MariaDB, SQL Server, SQLite                        |
| ------------------------------------- | --------------------------------------------------------- | -------------------------------------------------- |
| Add a value                           | `ADD VALUE`, at the place of the value in the enum        | The column accepts the value.                      |
| Remove or reorder values              | The migration replaces the type and converts the columns. | The migration changes the column or its check.     |
| Rename a value of a column of names   | `RENAME VALUE`, which renames the value in the rows too   | Leadseal stops with an error.                      |
| Rename a value of a column of numbers | Nothing changes, because the rows hold the number.        | Nothing changes, because the rows hold the number. |

A migration that removes a value fails if a row holds the value.
A PostgreSQL transaction cannot use a value that it adds.
So when a migration also defaults a column to a new value, PostgreSQL replaces the type instead of adding the value.

To leave out a value, such as `STATE_UNSPECIFIED`, give the field a `check`, such as `state <> 'STATE_UNSPECIFIED'`.
Refer to [Column options](column-options.md).

The PostgreSQL JDBC driver sends a Java `String` as `varchar`, which an enum column does not accept.
Add `stringtype=unspecified` to the connection URL, or write a cast, such as `CAST(? AS order_line_state)`.

## Database types

| Column type | PostgreSQL         | MariaDB         | SQL Server         | SQLite    |
| ----------- | ------------------ | --------------- | ------------------ | --------- |
| `BOOLEAN`   | `boolean`          | `BOOLEAN`       | `BIT`              | `INTEGER` |
| `INT`       | `integer`          | `INT`           | `INT`              | `INTEGER` |
| `BIGINT`    | `bigint`           | `BIGINT`        | `BIGINT`           | `INTEGER` |
| `REAL`      | `real`             | `FLOAT`         | `REAL`             | `REAL`    |
| `DOUBLE`    | `double precision` | `DOUBLE`        | `FLOAT`            | `REAL`    |
| `DECIMAL`   | `numeric(p, s)`    | `DECIMAL(p, s)` | `DECIMAL(p, s)`    | `TEXT`    |
| `TEXT`      | `text`             | `LONGTEXT`      | `NVARCHAR(MAX)`    | `TEXT`    |
| `TEXT(n)`   | `varchar(n)`       | `VARCHAR(n)`    | `NVARCHAR(n)`      | `TEXT`    |
| `BYTES`     | `bytea`            | `LONGBLOB`      | `VARBINARY(MAX)`   | `BLOB`    |
| `BYTES(n)`  | `bytea`            | `VARBINARY(n)`  | `VARBINARY(n)`     | `BLOB`    |
| `UUID`      | `uuid`             | `UUID`          | `UNIQUEIDENTIFIER` | `TEXT`    |
| `TIMESTAMP` | `timestamptz`      | `DATETIME(6)`   | `DATETIME2(6)`     | `TEXT`    |
| `DATE`      | `date`             | `DATE`          | `DATE`             | `TEXT`    |
| `TIME`      | `time`             | `TIME(6)`       | `TIME(6)`          | `TEXT`    |
| `JSON`      | `jsonb`            | `JSON`          | `NVARCHAR(MAX)`    | `TEXT`    |
| `ENUM`      | the enum type      | `ENUM(...)`     | `NVARCHAR(63)`     | `TEXT`    |

`TEXT(n)` and `BYTES(n)` are columns with a `length`.
`ENUM` is an enum column that stores names, as [Enum columns](#enum-columns) describes.
SQL Server writes `NVARCHAR(MAX)` for a length above `4000`, and `VARBINARY(MAX)` for a length above `8000`.
SQLite does not enforce lengths.

MariaDB and SQL Server store `TIMESTAMP` values without a time zone.
Migrations write timestamp defaults in UTC.

SQLite stores decimals as text, which keeps their digits.
SQLite compares and sorts such columns as text.

## Limits

| Limit                                   | PostgreSQL | MariaDB                        | SQL Server                        | SQLite |
| --------------------------------------- | ---------- | ------------------------------ | --------------------------------- | ------ |
| Decimal precision                       | `1000`     | `65`                           | `38`                              | none   |
| Decimal scale                           | `1000`     | `38`                           | `38`                              | none   |
| Types that a key or an index cannot use | none       | `LONGTEXT`, `LONGBLOB`, `JSON` | `NVARCHAR(MAX)`, `VARBINARY(MAX)` | none   |

A key or an index is a primary key, an index or a foreign key.
Leadseal stops with an `UnsupportedChangeException` when a column breaks a limit of a dialect that you choose.

## Zero values

When a migration adds a column that does not accept `NULL` to a table with rows, the existing rows get the default of the column.
A column without a default gets the zero value of its type.
When a column stops accepting `NULL`, the migration first sets its `NULL` values to the default, or to the zero value.

| Column type                                  | Zero value                             |
| -------------------------------------------- | -------------------------------------- |
| `BOOLEAN`                                    | `false`                                |
| `INT`, `BIGINT`, `REAL`, `DOUBLE`, `DECIMAL` | `0`                                    |
| `TEXT`                                       | an empty text                          |
| `BYTES`                                      | no bytes                               |
| `UUID`                                       | `00000000-0000-0000-0000-000000000000` |
| `TIMESTAMP`                                  | `1970-01-01T00:00:00Z`                 |
| `DATE`                                       | `1970-01-01`                           |
| `TIME`                                       | `00:00:00`                             |
| `JSON`                                       | `{}`                                   |
