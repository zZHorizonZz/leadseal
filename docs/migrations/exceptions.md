---
description: The leadseal exceptions, each of which names who has to fix the problem: the schema, a database, the migration history or the build.
content_type: reference
---

# Exceptions

Every problem that stops leadseal is a `SchemaException`, in the package `dev.horizon.leadseal`.
Each kind of problem is its own subclass, so your code can tell who has to fix it.
The protoc plugin prints the message after `Migration generation failed:`.
Refer to [Generate migrations from Java](generate-migrations-from-java.md).

## Kinds

| Name                         | Type          | Thrown when                                                                                                       | Fix                                                                                                                        |
| ---------------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `InvalidSchemaException`     | `final` class | A table, column, attribute, type or expression breaks a rule, or a protobuf option is invalid                     | Change the schema. Refer to [Troubleshooting schemas](../schemas/troubleshooting-schemas.md).                              |
| `UnsupportedChangeException` | `final` class | A database cannot do what the schema or a change asks for                                                         | Change the schema, or leave out the dialect. Refer to [Troubleshooting dialects](../dialects/troubleshooting-dialects.md). |
| `BrokenHistoryException`     | `final` class | The migrations do not form one chain, or a migration or snapshot file is missing, unreadable or of another format | Repair the migration directory. Refer to [Troubleshooting migrations](troubleshooting-migrations.md).                      |
| `ConfigurationException`     | `final` class | No installed dialect has a name, no dialect is given, or a plugin option is invalid                               | Change the build. Refer to [Troubleshooting migrations](troubleshooting-migrations.md).                                    |

`SchemaException` is an abstract `RuntimeException`.
Catch it to handle every kind.

## Examples

| Name                         | Message                                                                                                              |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `InvalidSchemaException`     | `The table message shop.v1.Order has no primary key. Set (leadseal.v1.column).primary_key on its key fields.`        |
| `UnsupportedChangeException` | `MariaDB has no partial indexes, so it cannot create the index 'ux_orders_customer_id' with the condition line = 1.` |
| `BrokenHistoryException`     | `The snapshot db/migration/snapshots/298c2267bb456adc/contract.json is missing.`                                     |
| `ConfigurationException`     | `No installed dialect is named 'oracle'. Installed dialects: [mariadb, postgresql, sqlite, sqlserver].`              |
