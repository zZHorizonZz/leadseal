---
description: A generated column holds the value of an expression of its row, such as a total or a label, which the database writes on each insert and update.
content_type: concept
---

# About generated columns

A generated column holds the value of an expression of the other columns of its row.
The database writes the value on each insert and each update, and stores it, so a query reads it like any other column.
Use it for a value that always follows from the row, such as a total, a label in upper case, or a full name.

```proto
message Order {
  option (leadseal.v1.table) = {};

  int64 id = 1 [(leadseal.v1.column).primary_key = true];
  int64 price = 2;
  int32 quantity = 3;
  optional string note = 4;
  int64 total = 5 [(leadseal.v1.column).generated = "price * quantity"];
  string label = 6 [(leadseal.v1.column).generated = "upper(note)"];
}
```

After `INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift')`, the row has the total `1000` and the label `GIFT`.
An update of `price`, `quantity` or `note` writes them again.
A statement cannot write a generated column itself: the database refuses it.

## Generated columns and rewrites

A generated column always has the value of its expression, from the insert of the row on.
A [rewrite](about-rewrites.md) runs only on updates, can read the row before the update as `old.<field>`, and can call `now()`, so it fits values such as the time of the last change or a revision number.
For a value that follows from the row, such as a label of a slug, use a generated column: a rewrite would leave the label at its default until the first update.

## What a generated column reads

The value is an expression in the same subset of [CEL](https://cel.dev) as a rewrite, and has the type of the column.
Refer to [About rewrites](about-rewrites.md#what-a-rewrite-reads) for what an expression can use.
A generated column has these limits, so that every database can store it:

- It reads only the fields of its own row: no `old.` or `new.`, and no row of another table.
- It reads no other generated field.
- It calls no `now()` or `uuid()`, because the value must be the same each time the database works it out for the same row.

The column accepts `NULL`, whatever the presence of its field, and has `NULL` where its expression gives `NULL`, as `label` does for an order without a note.
It has no default, no identity and no collation, and it cannot be in a primary key, an index or a foreign key.
A rewrite cannot write or read a generated column, and a trigger cannot write one.

## How the database writes a generated column

| Database   | Column                                                  |
| ---------- | ------------------------------------------------------- |
| PostgreSQL | `total bigint GENERATED ALWAYS AS (price * quantity) STORED` |
| MariaDB    | `total BIGINT GENERATED ALWAYS AS (price * quantity) STORED` |
| SQLite     | `total INTEGER GENERATED ALWAYS AS (price * quantity) STORED` |
| SQL Server | `total AS CAST((price * quantity) AS BIGINT) PERSISTED`  |

SQL Server gives a computed column the type of its expression, so the cast keeps the type of the field.

## Migrations

A migration does not change a generated column in place.
When the expression changes, or a column that it reads is renamed or changes its type, the migration drops the generated column before the other changes and adds it again after them.
Adding the column writes the value of the expression into every row, so nothing is lost.
SQLite cannot add a stored generated column to a table, so it rebuilds the table.

A field that becomes generated loses the values that it had: the migration drops the column, with a warning, and adds it as a generated column.
A generated field cannot become a field that is not generated, because the column would have no values to keep.
Add a new field for the values instead.

## Related

- [About rewrites](about-rewrites.md)
- [Column options](column-options.md)
- [Troubleshooting schemas](troubleshooting-schemas.md)
