---
description: A trigger runs actions after each insert, update or delete of a row, such as writing a log row, keeping a count in another table, or refusing the change.
content_type: concept
---

# About triggers

A trigger runs actions after each insert, update or delete of a row of its table, for the rows that meet its condition.
An action writes the rows of another table, or refuses the mutation with a message.

```proto
message Order {
  option (leadseal.v1.table) = {
    trigger: {
      name: "log_state"
      on: [EVENT_UPDATE]
      when: "changed(state)"
      actions: [{
        insert: {
          message: "shop.v1.OrderEvent"
          values: [{key: "order_id" value: "id"}, {key: "state" value: "state"}]
        }
      }]
    }
    trigger: {
      name: "keep_paid"
      on: [EVENT_DELETE]
      actions: [
        {assert: {expression: "state != 'PAID'", message: "Paid orders cannot be deleted."}},
        {delete: {message: "shop.v1.OrderEvent", where: "order_id == old.id"}},
        {update: {message: "shop.v1.Customer", where: "id == old.customer_id", set: [{key: "orders" value: "orders - 1"}]}}
      ]
    }
  };

  int64 id = 1 [(leadseal.v1.column).primary_key = true];
  string state = 2;
  optional int64 customer_id = 3 [(leadseal.v1.column).foreign_key = {message: "shop.v1.Customer"}];
}
```

After `UPDATE orders SET state = 'SENT'`, each updated order has a new row in `order_events`.
A `DELETE` of a paid order fails with the message `Paid orders cannot be deleted.`, and the order stays.
A `DELETE` of another order deletes its events and counts one order less for its customer.

## Actions

| Action   | What it does                                                                                       |
| -------- | -------------------------------------------------------------------------------------------------- |
| `insert` | Writes a row into another table, with a value for each field of that table that has no default.    |
| `update` | Changes the rows of another table that meet its `where`, with a value for each field in `set`.     |
| `delete` | Deletes the rows of another table that meet its `where`.                                           |
| `assert` | Refuses the mutation when its condition is false. A condition that is `NULL` holds, as in a check. |

The actions of a trigger run in their order, for each row of the mutation.
An `assert` that fails stops the mutation, and the database rolls back what the actions before it changed.

In an `update` or a `delete`, a field name reads the row of the other table, and `new.<field>` and `old.<field>` read the row of the trigger.
In `where: "id == old.customer_id"`, `id` is the id of the customer, and `old.customer_id` is the customer of the deleted order.
An `update` or a `delete` needs a `where`, so that it never changes every row of the other table by accident.
An `insert` or an `update` cannot write an identity column, because the database writes its values.

A trigger cannot change its own table.
To change the values of the row that a mutation writes, use a rewrite.
Refer to [About rewrites](about-rewrites.md).

## What a trigger reads

The condition and the values of an `insert` or an `assert` are expressions in the same subset of CEL as a rewrite.
The `where` and the values of an `update` or a `delete` read the other table, as [Actions](#actions) describes.
Refer to [About rewrites](about-rewrites.md#what-a-rewrite-reads).

| Name          | Value                                                                                             |
| ------------- | ------------------------------------------------------------------------------------------------- |
| `state`       | The field after the mutation: the new row of an insert or an update, and the old row of a delete. |
| `new.state`   | The field in the new row, on an insert or an update.                                              |
| `old.state`   | The field in the old row, on an update or a delete.                                               |
| `author.name` | A field of the row that the key `author_id` references in another table.                          |

A field whose name ends with `_id` and that has a foreign key of one column links to the row that it references.
The link takes the name of the field without `_id`, so `author_id` gives `author`.
A message that already has a field with that name has no link of that name.
An expression reads the fields of the linked row, as in `author.active`, and is `NULL` when the key is `NULL` or no row has it.
It reads one link, not the links of the linked row, and reads it from the row itself, not as `new.author` or `old.author`.

A trigger reads the row with its rewritten values.
A trigger on more than one mutation reads only the rows that every mutation has: `old.<field>` on `[EVENT_UPDATE, EVENT_DELETE]`, but no `old.<field>` on `[EVENT_INSERT, EVENT_UPDATE]`.

## Order

On an update, the database first writes the rewrites, and then runs the triggers of the update.
The triggers of a mutation run in the order of the table option.
An `assert` that fails stops the mutation, and the database rolls back the rows that it changed.

An action that writes the rows of another table runs the triggers of that table.
An `insert` runs the insert triggers of the other table, an `update` its update triggers, and a `delete` its delete triggers.
Triggers that write in a loop, such as an insert of an order that writes an event whose insert trigger updates the order, stop the build.
Triggers that only write each other's tables on other mutations, such as a delete of an author that deletes its posts and an insert of a post that updates its author, can never run each other, so they are fine.

## How the database writes a trigger

Each table has one database trigger for each mutation with triggers, `tg_<table>_<mutation>`, which runs after the mutation.
PostgreSQL and MariaDB write the rewrites in a trigger of their own, `rw_<table>`, before the update.
SQLite and SQL Server write them at the start of `tg_<table>_update`.

| Database   | `assert` fails with                              |
| ---------- | ------------------------------------------------ |
| PostgreSQL | `RAISE EXCEPTION` with the code `P0001`          |
| MariaDB    | `SIGNAL SQLSTATE '45000'`                        |
| SQLite     | `RAISE(ABORT, …)`, a `SQLITE_CONSTRAINT_TRIGGER` |
| SQL Server | `THROW 50000`                                    |

Each error has the message of the `assert`.

PostgreSQL runs the triggers of a row at the end of the statement, and SQL Server runs them once for all the rows of the statement, so they read the table after the whole statement.
On SQL Server, an `update` or a `delete` runs once for each row of the statement, so two inserted lines of one order count two, as on the other databases.
MariaDB and SQLite run them after each row, so a trigger that reads other rows of its table during a statement that changes many rows reads them partly changed.

A migration that renames a column or a table that the triggers read or write, changes the primary key, or changes a column of the table, drops the database triggers before its other statements and creates them again after them.
So the statements of the migration, such as the update that fills a column that becomes NOT NULL, run no trigger.
Refer to [About database differences](../dialects/about-database-differences.md).

## Related

- [Table options](table-options.md#trigger)
- [About rewrites](about-rewrites.md)
- [Troubleshooting schemas](troubleshooting-schemas.md)
