# Behavior cases

Each folder here is a suite: versions of a schema, and behavior files that run on them.
`BehaviorTest` runs every behavior file on PostgreSQL, MariaDB, SQL Server and SQLite, each on a new database.

```
triggers/
  v1/shop.proto        a version of the schema, in the package triggers.v1
  v2/shop.proto        the next version, in the package triggers.v2
  insert-row.sql       a behavior
```

The versions of a suite differ only in their version part, so a message has the same table id in each, and a migration from one version to another changes the table instead of creating a new one.

## Behavior files

A behavior file is SQL with directives in comments.
The SQL is the same on every database, so it uses lowercase names without quotes, and `'1'` and `'0'` for booleans.

```sql
-- Comments that are not directives describe the behavior.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'PAID');
DELETE FROM orders;
-- error: Paid orders stay.
-- expect: SELECT id, state FROM orders ORDER BY id
1 | PAID
```

| Directive | What it does |
| --------- | ------------ |
| `-- databases: <database>, ...` | Runs the behavior only on these databases: `postgresql`, `mariadb`, `sqlserver` or `sqlite`. It comes before the steps. |
| `-- migrate: <version>` | Generates the migration to the version of the schema, and applies it with Flyway. A behavior starts with one. `-- migrate: <version> allow_unsafe` allows changes that can lose values. |
| `-- error: <message>` | The statement or the migration before it must fail with an error that contains the message. Without a message, any error of the database passes. |
| `-- expect: <query>` | The query must give the rows on the lines after it, in their order, up to a blank line. Cells are split by `\|`, `NULL` is a null and `''` is an empty text. No lines means no rows. |
| `-- only: <database>, ...` | Runs the next statement or directive only on these databases. |

A statement ends with a semicolon at the end of a line.
A failure names the file and the line of the step.

## Values

Booleans read as `1` and `0`, and bytes as hex.
`literal(<type>, '<value>')` in a statement or a query is the literal of the database for a value in its canonical text, for a type whose literals differ between databases: `literal(timestamp, '2026-10-14T07:30:00Z')`, `literal(bytes, '0aff')` or `literal(json, '{"a": 1}')`.
The types are `bool`, `int32`, `int64`, `float`, `double`, `decimal`, `string`, `bytes`, `uuid`, `timestamp`, `date`, `time` and `json`.

## The catalog

A query of the catalog reads it through JDBC, so it is the same on every database:

| Query | Rows |
| ----- | ---- |
| `columns(<table>)` | Each column in order, with `required` or `optional`. |
| `primary key(<table>)` | Each column of the primary key, in key order. |
| `indexes(<table>)` | Each index named `ix_` or `ux_`, with `index` or `unique`. |
| `foreign keys(<table>)` | Each foreign key column, as `customer_id -> customers.id`. |
| `comment(<table>)`, `comment(<table>, <column>)` | The comment, or no row. |

A table in a schema is `<schema>.<table>`.
