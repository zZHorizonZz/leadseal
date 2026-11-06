---
description: Find the cause and the fix of leadseal errors about table messages, fields and their options.
content_type: troubleshooting
---

# Troubleshooting schemas

Leadseal checks the schema before it writes a migration.
A problem in a table message or a field stops the build with an `InvalidSchemaException`.
protoc prints its message after `--leadseal_out: Migration generation failed:`.
For errors about the migration directory or the plugin options, refer to [Troubleshooting migrations](../migrations/troubleshooting-migrations.md).

## The table message ... has no primary key. Set (leadseal.v1.column)...

```text
--leadseal_out: Migration generation failed: The table message shop.v1.Order has no primary key. Set (leadseal.v1.column).primary_key on its key fields.
```

**Cause**: No field of the table message has `primary_key: true`.

**Fix**: Set `[(leadseal.v1.column).primary_key = true]` on the key fields.
Refer to [Column options](column-options.md).

**Verify**: The build writes a migration with `CONSTRAINT "pk_orders" PRIMARY KEY`.

## The field ... cannot have the column type ..., because its values are ...

```text
--leadseal_out: Migration generation failed: The field shop.v1.Order.id cannot have the column type UUID, because its values are 64-bit integer.
```

**Cause**: The `type` option of the field cannot hold all the values of the field type.

**Fix**: Choose a type that the field type allows, or change the field type.
Refer to [Column types](column-types.md#other-column-types-of-a-field).

**Verify**: The build writes no error.

## The foreign key of ... references ..., which is not a table message...

```text
--leadseal_out: Migration generation failed: The foreign key of shop.v1.Order.customer_id references Customer, which is not a table message. Use the full message name of a message with the (leadseal.v1.table) option.
```

**Cause**: The `message` of a foreign key is not the full name of a table message in the files that protoc compiles.

**Fix**: Write the full message name, such as `shop.v1.Customer`, and give that message the `(leadseal.v1.table)` option.

**Verify**: The migration has a `FOREIGN KEY` that references the table.

## The primary key column ... must not accept NULL.

```text
--leadseal_out: Migration generation failed: The primary key column 'orders.id' must not accept NULL.
```

**Cause**: A primary key field has presence, or `nullable: true`.
A field with the `optional` label or a message type has presence.

**Fix**: Remove the `optional` label, or set `nullable: false` on the field.

**Verify**: The build writes no error.

## The message ... has no field ...

```text
--leadseal_out: Migration generation failed: The message shop.v1.Order has no field 'customer'.
```

**Cause**: An index, a check or a foreign key of the table names a field that the message does not have, or a field with `ignore: true`.

**Fix**: Use the field name, not the column name, of a field that has a column.

**Verify**: The build writes no error.

## The field ... has both a default_value and a generated default...

```text
--leadseal_out: Migration generation failed: The field shop.v1.Order.created has both a default_value and a generated default. Set one of them.
```

**Cause**: A `uuid`, `timestamp` or `date` option sets `default_value` and also `default_random`, `default_now` or `default_today`.

**Fix**: Remove one of the two.

**Verify**: The build writes no error.

## The default ... of the field ... is not a value of ...

```text
--leadseal_out: Migration generation failed: The default 'SHIPPED' of the field shop.v1.Order.state is not a value of shop.v1.State. Use a value name.
```

**Cause**: The default of an enum field is not a value of the enum.
A `text` option takes a value name, and an `integer` option takes a value number.

**Fix**: Use a name or a number of the enum, as the message says.

**Verify**: The build writes no error.

## The default ... of the column ... is not a value of its type...

```text
--leadseal_out: Migration generation failed: The default 'tomorrow' of the column 'due' is not a value of its type date. An example of a value is 2026-10-14.
```

**Cause**: A text default, such as the `default_value` of a `date`, `time`, `uuid` or `decimal` option, is not a value of the column type.

**Fix**: Write the default as the example in the message shows.

**Verify**: The migration has the `DEFAULT` of the column.

## The field ... is a google.type.Decimal, so it needs a decimal type option...

```text
--leadseal_out: Migration generation failed: The field shop.v1.Order.price is a google.type.Decimal, so it needs a decimal type option with a precision.
```

**Cause**: A `google.type.Decimal` field has no `decimal` option.

**Fix**: Set `[(leadseal.v1.column).decimal = {precision: 12, scale: 2}]`.

**Verify**: The migration has a `numeric(12, 2)` or `DECIMAL(12, 2)` column.

## The foreign key of ... needs a field, because ... has ... primary key fields.

```text
--leadseal_out: Migration generation failed: The foreign key of shop.v1.Shipment.order_id needs a field, because shop.v1.OrderLine has 2 primary key fields.
```

**Cause**: A foreign key without `field` references a message whose primary key has more than one field.

**Fix**: Set `field`, or use a `foreign_key` of the table with every key field.
Refer to [Table options](table-options.md).

**Verify**: The migration has the foreign key.

## The enum ... renames the value ... to ..., but the columns of the ... dialect store the value names.

```text
--leadseal_out: Migration generation failed: The enum order_state renames the value STATE_SENT to STATE_SHIPPED, but the columns of the mariadb dialect store the value names. Keep the name STATE_SENT, or store the value numbers with the integer type option.
```

**Cause**: An enum value kept its number but got another name, and the dialect has no enum type to rename the value in.
The rows hold the old name.
PostgreSQL renames the value, but MariaDB, SQL Server and SQLite cannot.
Leadseal throws an `UnsupportedChangeException`.

**Fix**: Keep the old name, and add the new name as an alias with `option allow_alias = true`.
To rename the value, update the rows in a migration of your own first.

**Verify**: The build writes no error.

## The schema has two enums named ... with other values.

```text
--leadseal_out: Migration generation failed: The schema has two enums named order_state with other values. Give one of them another name.
```

**Cause**: Two enums have the same path in their packages, such as `Order.State` in `shop.v1` and in `billing.v1`, but other values.
The columns of an enum share its name, and on PostgreSQL its type.

**Fix**: Rename or move one of the enums, or give both the same values.

**Verify**: The build writes no error.

## The schema has the enum ... and a table with the same name.

```text
--leadseal_out: Migration generation failed: The schema has the enum order_state and a table with the same name. Give one of them another name.
```

**Cause**: An enum, such as `Order.State`, has the name of a table, such as the table of a message `OrderState`.
PostgreSQL gives each table a type with its name, so the enum type would clash with it.

**Fix**: Rename the enum, or set another `name` for the table.

**Verify**: The build writes no error.

## The migration changes the column ... from ... to ..., which can lose data and is unsafe.

```text
--leadseal_out: Migration generation failed: The migration changes the column orders.state from text(50) to text(20), which can lose data and is unsafe. Keep a type that holds every value of text(50), or set allow_unsafe=true to apply the change.
```

**Cause**: The new type of the column holds fewer values than the old one, such as a shorter text, a smaller integer, an enum without a value, or another kind of type.
Leadseal throws an `UnsupportedChangeException`.

**Fix**: Keep a type that holds every value, such as `text(50)` or a longer text.
To change the type anyway, set `allow_unsafe=true` for one build, after you check the rows.
Refer to [About data loss](../migrations/about-data-loss.md).

**Verify**: The build writes no error.

## The expression ... is not valid: ...

```text
--leadseal_out: Migration generation failed: The expression ''first'' is not valid: expected an expression of type int but it is of type string (at character 1).
```

**Cause**: An expression, such as a rewrite, does not parse, reads a field that the message does not have, or gives a value of another type than its column.
The character is where the problem starts.

**Fix**: Correct the expression, and give it the type of the column, such as `old.revision + 1` for an integer column.
Refer to [About rewrites](about-rewrites.md#what-a-rewrite-reads).

**Verify**: The build writes no error.

## The rewrites read each other: ...

```text
--leadseal_out: Migration generation failed: The rewrites read each other: slug -> label -> slug.
```

**Cause**: The rewrite of one field reads a field whose rewrite reads the first field, directly or through other rewrites.
Leadseal cannot tell which one to write first.

**Fix**: Let one of the rewrites read the value before the update, such as `old.label`, or remove the loop.

**Verify**: The build writes no error.

## A rewrite of the table ... writes the column ..., which is part of the primary key.

```text
--leadseal_out: Migration generation failed: A rewrite of the table orders writes the column id, which is part of the primary key. A rewrite cannot change the key of a row.
```

**Cause**: A primary key field has a `rewrite`.

**Fix**: Remove the rewrite from the key field.

**Verify**: The build writes no error.

## The table ... has a field named new or old, so an expression of a trigger cannot read it.

```text
--leadseal_out: Migration generation failed: The table posts has a field named new or old, so an expression of a trigger cannot read it.
```

**Cause**: A rewrite reads fields as `new.<field>` and `old.<field>`, so a field named `new` or `old` would be ambiguous.

**Fix**: Rename the field, and keep its column name with the `name` column option.

**Verify**: The build writes no error.

## A trigger of the table ... writes into its own table.

```text
--leadseal_out: Migration generation failed: The trigger orders.copy is not valid. A trigger of the table orders writes into its own table. A trigger cannot change the rows of its table: use a rewrite for the values of the row.
```

**Cause**: An `insert` of a trigger names the message of its own table.

**Fix**: To change the values of the row, use a `rewrite` on the column.
To keep a history, write the rows into another table.
Refer to [About rewrites](about-rewrites.md).

**Verify**: The build writes no error.

## The triggers write in a loop: ...

```text
--leadseal_out: Migration generation failed: The triggers write in a loop: orders.log -> order_events.touch -> orders. A trigger that writes a row runs the triggers of that table, so the loop would never end.
```

**Cause**: A trigger writes a row into a table whose triggers write, directly or through others, into the first table.

**Fix**: Remove one of the `insert` actions of the loop.

**Verify**: The build writes no error.

## A trigger of the table ... writes a row into ... without the column ..., which is NOT NULL and has no default.

```text
--leadseal_out: Migration generation failed: The trigger orders.log is not valid. A trigger of the table orders writes a row into order_events without the column kind, which is NOT NULL and has no default. Give it a value.
```

**Cause**: The `insert` of a trigger has no value for a field of the other message that needs one.

**Fix**: Add the field to `values`, or give it a default.

**Verify**: The build writes no error.

## The trigger ... of the message ... has no action.

```text
--leadseal_out: Migration generation failed: The trigger log of the message shop.v1.Order has no action. Set actions to insert, update, delete or assert actions.
```

**Cause**: A trigger has no `actions`.

**Fix**: Give the trigger at least one action.
Refer to [Table options](table-options.md#trigger).

**Verify**: The build writes no error.

## An update or a delete of the trigger ... has no where.

```text
--leadseal_out: Migration generation failed: An update or a delete of the trigger count of the message shop.v1.Order has no where. Set where to the condition of the rows, such as id == new.order_id.
```

**Cause**: An `update` or a `delete` action has no `where`.
Without it, the action would change or delete every row of the other table.

**Fix**: Set `where` to the condition of the rows to change, such as `id == new.customer_id`.

**Verify**: The build writes no error.

## The expression ... is not valid: '...' is an edge; read only the fields of the row 'new'.

```text
--leadseal_out: Migration generation failed: The expression 'new.author.active' is not valid: 'author' is an edge; read only the fields of the row 'new' (at character 5).
```

**Cause**: An expression reads a linked row through `new.` or `old.`.

**Fix**: Read the link from the row itself, as in `author.active`.
On an insert or an update it is the new row, and on a delete the old row.
Refer to [About triggers](about-triggers.md#what-a-trigger-reads).

**Verify**: The build writes no error.

## The identity column ... must be an integer column...

```text
--leadseal_out: Migration generation failed: The identity column 'id' must be an integer column that does not accept NULL and has no default.
```

**Cause**: A field with `identity: true` is nullable or has a default.

**Fix**: Remove the default, and make the field not accept `NULL`.

**Verify**: The migration writes the identity of the column.

## The foreign key of ... has an action that this version of leadseal does not know...

```text
--leadseal_out: Migration generation failed: The foreign key of shop.v1.Order.customer_id has an action that this version of leadseal does not know. Use NO_ACTION, RESTRICT, CASCADE or SET_NULL.
```

**Cause**: The `on_delete` or `on_update` of a foreign key is a number that `Action` has no value for, from a newer `options.proto`.

**Fix**: Use one of the actions of [Column options](column-options.md#action), or update leadseal.

**Verify**: The build writes no error.
