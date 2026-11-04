---
description: Every field of the type oneof of the leadseal column option, which sets the column type, its length and its default.
content_type: reference
---

# Column type options

The `type` oneof of `(leadseal.v1.column)` sets the column type and its options.
Without it, the type follows from the field type.
Refer to [Column types](column-types.md).

```proto
string id = 1 [(leadseal.v1.column) = {primary_key: true, uuid: {default_random: true}}];
string price = 2 [(leadseal.v1.column).decimal = {precision: 12, scale: 2}];
```

## `(leadseal.v1.column).type`

Set one field of the `type` oneof to choose the column type and its options.
A field can only have a type that holds all its values.
Refer to [Column types](column-types.md).

| Name | Type | Default | Description |
| --- | --- | --- | --- |
| `boolean.default_value` | `optional bool` | none | The default value. |
| `integer.format` | `FORMAT_INT32` or `FORMAT_INT64` | follows from the field type | The size of the integer. |
| `integer.default_value` | `optional int64` | none | The default value. For an enum field, a value number. |
| `integer.identity` | `bool` | `false` | Whether the database generates the values, for a primary key of one field. |
| `number.format` | `FORMAT_FLOAT` or `FORMAT_DOUBLE` | follows from the field type | The size of the number. |
| `number.default_value` | `optional double` | none | The default value. |
| `decimal.precision` | `uint32` | none | The number of digits. |
| `decimal.scale` | `uint32` | `0` | The number of digits after the decimal point. |
| `decimal.default_value` | `optional string` | none | The default value, such as `"0.00"`. |
| `text.length` | `uint32` | `0`, no limit | The maximum number of characters. |
| `text.default_value` | `optional string` | none | The default value. For an enum field, a value name. |
| `text.collation` | `map<string, string>` | none | The collation for each database, by dialect name, such as `{key: "mariadb" value: "utf8mb4_bin"}`. |
| `bytes.length` | `uint32` | `0`, no limit | The maximum number of bytes. |
| `bytes.default_value` | `optional bytes` | none | The default value. |
| `uuid.default_value` | `optional string` | none | The default value, such as `"123e4567-e89b-12d3-a456-426614174000"`. |
| `uuid.default_random` | `bool` | `false` | Whether the database generates a random UUID for each new row. |
| `timestamp.default_value` | `optional google.protobuf.Timestamp` | none | The default value. |
| `timestamp.default_now` | `bool` | `false` | Whether the database sets new rows to the current time. |
| `date.default_value` | `optional string` | none | The default value, such as `"2026-10-14"`. |
| `date.default_today` | `bool` | `false` | Whether the database sets new rows to the current date in UTC. |
| `time.default_value` | `optional string` | none | The default value, such as `"07:30:00"`. |
| `json.default_value` | `optional string` | none | The default value, such as `"{}"`. |

Set either `default_value` or the generated default of a type, not both.
A text or bytes column in a key or an index needs a `length` on MariaDB and SQL Server.
SQL Server cannot add or remove `identity` on an existing column.
A database without a `collation` entry uses its default collation.
