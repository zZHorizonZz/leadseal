---
description: Find the cause and the fix of leadseal errors about changes that a database cannot make.
content_type: troubleshooting
---

# Troubleshooting dialects

A change that a database of your `dialect` options cannot make stops the build with an `UnsupportedChangeException`.
Leadseal writes no file for that migration.
For what each database supports, refer to [Dialect support](dialect-support.md).

## ... cannot use the column ... in a key or an index, because its type is ...

```text
--leadseal_out: Migration generation failed: MariaDB cannot use the column orders.state in a key or an index, because its type is LONGTEXT. Give the column a length.
```

**Cause**: A text, bytes or JSON column without a length is in a primary key, an index or a foreign key.
MariaDB and SQL Server cannot index such a column.

**Fix**: Set a `length` on the field, as the message says.
On SQL Server, use at most `4000` for text and `8000` for bytes.

**Verify**: The migration writes `VARCHAR(n)` or `NVARCHAR(n)` for the column.

## SQLite has no namespaces, so ...

```text
--leadseal_out: Migration generation failed: SQLite has no namespaces, so the table 'orders' cannot be in the namespace 'sales'.
```

**Cause**: A table message has a `schema` option, and `sqlite` is one of the dialects.

**Fix**: Remove the `schema` option, or leave out the `sqlite` dialect.

**Verify**: The build writes the SQLite SQL with no error.

## MariaDB has no partial indexes, so it cannot create the index ...

```text
--leadseal_out: Migration generation failed: MariaDB has no partial indexes, so it cannot create the index 'ux_orders_customer_id' with the condition line = 1.
```

**Cause**: An index has a `where` condition, and `mariadb` is one of the dialects.

**Fix**: Remove the `where` condition, or leave out the `mariadb` dialect.

**Verify**: The build writes the MariaDB SQL with no error.

## SQL Server cannot add IDENTITY to the existing column ...

```text
--leadseal_out: Migration generation failed: SQL Server cannot add IDENTITY to the existing column orders.id. Add a new column or a new table instead.
```

**Cause**: An existing column gets `identity: true`, and `sqlserver` is one of the dialects.

**Fix**: Add a new column or a new table with the identity, and copy the rows in a migration of your own.

**Verify**: The build writes the SQL Server SQL with no error.

## SQL Server cannot remove IDENTITY from the existing column ...

```text
--leadseal_out: Migration generation failed: SQL Server cannot remove IDENTITY from the existing column orders.id. Add a new column or a new table instead.
```

**Cause**: An existing identity column loses `identity: true`, and `sqlserver` is one of the dialects.

**Fix**: Keep the identity, or add a new column without it.

**Verify**: The build writes the SQL Server SQL with no error.

## ... cannot store the column ... with a precision of ...

```text
--leadseal_out: Migration generation failed: SQL Server cannot store the column orders.total with a precision of 40 and a scale of 2. The maximum precision is 38 and the maximum scale is 38.
```

**Cause**: The `precision` or `scale` of a `decimal` option is above the limit of a database.

**Fix**: Use a precision and a scale within the limits of every database you choose.
Refer to [Column types](../schemas/column-types.md#limits).

**Verify**: The migration writes `DECIMAL(p, s)` for the column.
