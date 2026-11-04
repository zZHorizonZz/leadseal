---
description: Apply leadseal migrations with Flyway by pointing a wildcard location at the SQL folders of one database.
content_type: how-to
---

# Apply migrations with Flyway

Each leadseal migration has one folder of SQL for each database.
Flyway finds the files of one database with a wildcard location.
Another migration tool works too, if it runs the files in version order.

## Steps

1. Add the Flyway module of your database: `flyway-database-postgresql`, `flyway-mysql` for MariaDB, or `flyway-sqlserver`.
   SQLite needs no extra module.

2. Set the Flyway location to the folders of your database:

   | Database | Location |
   | --- | --- |
   | PostgreSQL | `classpath:db/migration/**/postgresql` |
   | MariaDB | `classpath:db/migration/**/mariadb` |
   | SQL Server | `classpath:db/migration/**/sqlserver` |
   | SQLite | `classpath:db/migration/**/sqlite` |

   With the Flyway API:

   ```java
   Flyway.configure()
       .dataSource(url, user, password)
       .locations("classpath:db/migration/**/postgresql")
       .load()
       .migrate();
   ```

3. Run Flyway against the database.

## Check the result

The `flyway_schema_history` table of the database has one row for each migration folder.
Its `version` column is the folder name.

## Use Spring Boot

In Spring Boot, `{vendor}` in a location is the name of the database.
For these four databases, that name is the dialect name:

```properties
spring.flyway.locations=classpath:db/migration/**/{vendor}
```

## Read a directory outside the class path

Use a `filesystem:` location:

```properties
spring.flyway.locations=filesystem:db/migration/**/postgresql
```

## Run a script outside a transaction

Some SQLite migrations must run outside a transaction.
Leadseal writes a Flyway script configuration file next to such a script, such as `V20261014081951.sql.conf`:

```properties
executeInTransaction=false
```

Flyway reads this file.
With another migration tool, run that script outside a transaction.
Refer to [About database differences](../dialects/about-database-differences.md#sqlite).

## Related

- [Merge migration branches](merge-migration-branches.md)
- [About migration history](about-migration-history.md)
