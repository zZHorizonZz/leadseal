# Column types

Each column has a leadseal column type. Each dialect maps the column type to a database type.

## Field types

The column type follows from the protobuf field type:

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
| `google.protobuf.BoolValue`                                             | `BOOLEAN`   | Yes            |
| `google.protobuf.Int32Value`                                            | `INT`       | Yes            |
| `google.protobuf.UInt32Value`, `Int64Value`, `UInt64Value`              | `BIGINT`    | Yes            |
| `google.protobuf.FloatValue`                                            | `REAL`      | Yes            |
| `google.protobuf.DoubleValue`                                           | `DOUBLE`    | Yes            |
| `google.protobuf.StringValue`                                           | `TEXT`      | Yes            |
| `google.protobuf.BytesValue`                                            | `BYTES`     | Yes            |
| Another message                                                         | `JSON`      | Yes            |
| A repeated field or a map                                               | `JSON`      | Yes            |

A field has presence if it has the `optional` label, is a message field, or is in a `oneof`. An enum column stores
the name of the enum value.

## Change the column type

Set `(leadseal.v1.column).type` to store a field with another column type:

| Default column type | Other column types |
| ------------------- | ------------------ |
| `TEXT`              | `UUID`, `JSON`     |
| `BYTES`             | `UUID`             |
| `INT`               | `BIGINT`           |

```proto
string id = 1 [(leadseal.v1.column) = {primary_key: true, type: TYPE_UUID}];
```

## Database types

| Column type | PostgreSQL         | MariaDB        | SQL Server         | SQLite    |
| ----------- | ------------------ | -------------- | ------------------ | --------- |
| `BOOLEAN`   | `boolean`          | `BOOLEAN`      | `BIT`              | `INTEGER` |
| `INT`       | `integer`          | `INT`          | `INT`              | `INTEGER` |
| `BIGINT`    | `bigint`           | `BIGINT`       | `BIGINT`           | `INTEGER` |
| `REAL`      | `real`             | `FLOAT`        | `REAL`             | `REAL`    |
| `DOUBLE`    | `double precision` | `DOUBLE`       | `FLOAT`            | `REAL`    |
| `TEXT`      | `text`             | `LONGTEXT`     | `NVARCHAR(MAX)`    | `TEXT`    |
| `TEXT(n)`   | `varchar(n)`       | `VARCHAR(n)`   | `NVARCHAR(n)`      | `TEXT`    |
| `BYTES`     | `bytea`            | `LONGBLOB`     | `VARBINARY(MAX)`   | `BLOB`    |
| `BYTES(n)`  | `bytea`            | `VARBINARY(n)` | `VARBINARY(n)`     | `BLOB`    |
| `UUID`      | `uuid`             | `UUID`         | `UNIQUEIDENTIFIER` | `TEXT`    |
| `TIMESTAMP` | `timestamptz`      | `DATETIME(6)`  | `DATETIME2(6)`     | `TEXT`    |
| `JSON`      | `jsonb`            | `JSON`         | `NVARCHAR(MAX)`    | `TEXT`    |

`TEXT(n)` and `BYTES(n)` are columns with a `length`. SQL Server uses `NVARCHAR(MAX)` for a length above 4000 and
`VARBINARY(MAX)` for a length above 8000. SQLite does not enforce lengths.

MariaDB and SQL Server cannot index `LONGTEXT`, `LONGBLOB`, `NVARCHAR(MAX)` or `VARBINARY(MAX)` columns. Leadseal stops
with an error when such a column is in a primary key, an index or a foreign key. Set a `length` on the field.

## Required columns in existing tables

When a migration adds a column that does not accept `NULL` to a table, the existing rows get the zero value of the
column type: `0`, `false`, an empty text, an empty JSON object, the nil UUID or `1970-01-01 00:00:00` UTC. When a
column stops accepting `NULL`, the migration first sets its `NULL` values to the zero value.
