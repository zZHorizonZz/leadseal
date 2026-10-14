# Column types

Each column has a leadseal column type. Each dialect maps the column type to a database type.

## Field types

Without a `type` in the column option, the column type follows from the protobuf field type:

| Field type                                                              | Column type | Accepts `NULL` |
| ----------------------------------------------------------------------- | ----------- | -------------- |
| `bool`                                                                  | `BOOLEAN`   | With presence  |
| `int32`, `sint32`, `sfixed32`                                           | `INT`       | With presence  |
| `uint32`, `fixed32`, `int64`, `uint64`, `sint64`, `fixed64`, `sfixed64` | `BIGINT`    | With presence  |
| `float`                                                                 | `REAL`      | With presence  |
| `double`                                                                | `DOUBLE`    | With presence  |
| `string`                                                                | `TEXT`      | With presence  |
| An enum                                                                 | `TEXT`      | With presence  |
| `bytes`                                                                 | `BYTES`     | With presence  |
| `google.protobuf.Timestamp`                                             | `TIMESTAMP` | Yes            |
| `google.type.Date`                                                      | `DATE`      | Yes            |
| `google.type.TimeOfDay`                                                 | `TIME`      | Yes            |
| `google.type.Decimal`                                                   | `DECIMAL`   | Yes            |
| `google.protobuf.BoolValue`                                             | `BOOLEAN`   | Yes            |
| `google.protobuf.Int32Value`                                            | `INT`       | Yes            |
| `google.protobuf.UInt32Value`, `Int64Value`, `UInt64Value`              | `BIGINT`    | Yes            |
| `google.protobuf.FloatValue`                                            | `REAL`      | Yes            |
| `google.protobuf.DoubleValue`                                           | `DOUBLE`    | Yes            |
| `google.protobuf.StringValue`                                           | `TEXT`      | Yes            |
| `google.protobuf.BytesValue`                                            | `BYTES`     | Yes            |
| Another message                                                         | `JSON`      | Yes            |
| A repeated field or a map                                               | `JSON`      | Yes            |

A field has presence if it has the `optional` label, is a message field, or is in a `oneof`. Set the `nullable`
column option to choose otherwise, for example `nullable: false` for a `Timestamp` field that every row has. An enum
column stores the name of the enum value. A `DECIMAL` column needs a `decimal` type option with a precision.

## Choose another type

Set a field of the `type` oneof to store a field with another column type. A field can only have a type that holds
all its values:

| Field type                  | Other column types                                                       |
| --------------------------- | ------------------------------------------------------------------------ |
| `string`                    | `uuid`, `json`, `decimal`, `date`, `time`, `timestamp`                   |
| `bytes`                     | `uuid`                                                                   |
| An enum                     | `integer`, which stores the value number                                 |
| 32-bit integers             | `integer` with `FORMAT_INT64`                                            |
| `float`                     | `number` with `FORMAT_DOUBLE`                                            |

```proto
string id = 1 [(leadseal.v1.column) = {primary_key: true, uuid: {default_random: true}}];
string price = 2 [(leadseal.v1.column).decimal = {precision: 12, scale: 2}];
State state = 3 [(leadseal.v1.column).integer = {}];
```

## Database types

| Column type | PostgreSQL         | MariaDB          | SQL Server         | SQLite    |
| ----------- | ------------------ | ---------------- | ------------------ | --------- |
| `BOOLEAN`   | `boolean`          | `BOOLEAN`        | `BIT`              | `INTEGER` |
| `INT`       | `integer`          | `INT`            | `INT`              | `INTEGER` |
| `BIGINT`    | `bigint`           | `BIGINT`         | `BIGINT`           | `INTEGER` |
| `REAL`      | `real`             | `FLOAT`          | `REAL`             | `REAL`    |
| `DOUBLE`    | `double precision` | `DOUBLE`         | `FLOAT`            | `REAL`    |
| `DECIMAL`   | `numeric(p, s)`    | `DECIMAL(p, s)`  | `DECIMAL(p, s)`    | `TEXT`    |
| `TEXT`      | `text`             | `LONGTEXT`       | `NVARCHAR(MAX)`    | `TEXT`    |
| `TEXT(n)`   | `varchar(n)`       | `VARCHAR(n)`     | `NVARCHAR(n)`      | `TEXT`    |
| `BYTES`     | `bytea`            | `LONGBLOB`       | `VARBINARY(MAX)`   | `BLOB`    |
| `BYTES(n)`  | `bytea`            | `VARBINARY(n)`   | `VARBINARY(n)`     | `BLOB`    |
| `UUID`      | `uuid`             | `UUID`           | `UNIQUEIDENTIFIER` | `TEXT`    |
| `TIMESTAMP` | `timestamptz`      | `DATETIME(6)`    | `DATETIME2(6)`     | `TEXT`    |
| `DATE`      | `date`             | `DATE`           | `DATE`             | `TEXT`    |
| `TIME`      | `time`             | `TIME(6)`        | `TIME(6)`          | `TEXT`    |
| `JSON`      | `jsonb`            | `JSON`           | `NVARCHAR(MAX)`    | `TEXT`    |

`TEXT(n)` and `BYTES(n)` are columns with a `length`. SQL Server uses `NVARCHAR(MAX)` for a length above 4000 and
`VARBINARY(MAX)` for a length above 8000. SQLite does not enforce lengths.

The maximum decimal precision is 1000 on PostgreSQL, 65 on MariaDB and 38 on SQL Server. MariaDB allows a scale of at
most 38. SQLite has no exact decimal type, so it stores decimals as text, which keeps their digits. SQLite compares
and sorts such columns as text.

MariaDB and SQL Server store `TIMESTAMP` values without a time zone. Migrations write timestamp defaults in UTC.

MariaDB and SQL Server cannot index `LONGTEXT`, `LONGBLOB`, `NVARCHAR(MAX)` or `VARBINARY(MAX)` columns. Leadseal stops
with an error when such a column is in a primary key, an index or a foreign key. Set a `length` on the field.

## Required columns in existing tables

When a migration adds a column that does not accept `NULL` to a table with rows, the existing rows get the default of
the column. A column without a default gets the zero value of its type: `0`, `false`, an empty text, an empty JSON
object, the nil UUID, `1970-01-01` or `00:00:00`. When a column stops accepting `NULL`, the migration first sets its
`NULL` values to the default, or to the zero value.
