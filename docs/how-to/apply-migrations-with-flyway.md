# Apply migrations with Flyway

Each migration folder has one folder of SQL files for each database. Flyway finds the files of one database with a
wildcard location.

## Set the location

Set the Flyway location to the folders of your database:

| Database   | Location                                 |
| ---------- | ---------------------------------------- |
| PostgreSQL | `classpath:db/migration/**/postgresql`   |
| MariaDB    | `classpath:db/migration/**/mariadb`      |
| SQL Server | `classpath:db/migration/**/sqlserver`    |
| SQLite     | `classpath:db/migration/**/sqlite`       |

For a directory outside the classpath, use a `filesystem:` location, such as `filesystem:db/migration/**/postgresql`.

With the Flyway API:

```java
Flyway.configure()
    .dataSource(url, user, password)
    .locations("classpath:db/migration/**/postgresql")
    .load()
    .migrate();
```

In Spring Boot, `{vendor}` in a location is the name of the database. For these four databases, the name is the
same as the leadseal dialect name:

```properties
spring.flyway.locations=classpath:db/migration/**/{vendor}
```

Flyway also needs its module for the database: `flyway-database-postgresql`, `flyway-mysql` for MariaDB, or
`flyway-sqlserver`. SQLite needs no extra module.

## Scripts that run outside a transaction

Some SQLite migrations must run outside a transaction. Leadseal writes a Flyway script configuration file next to
such a script, for example `V20261014081951.sql.conf`, with this content:

```properties
executeInTransaction=false
```

Flyway reads this file. If you use another migration tool, run that script outside a transaction. Refer to
[Dialects](../reference/dialects.md).

## Merge branches

Two branches create migrations with different times. After you merge them, a database can have run the later
migration but not the earlier one. To let Flyway run the earlier migration, turn on `outOfOrder`:

```properties
spring.flyway.out-of-order=true
```

If leadseal reports two migrations that start from the same schema, refer to
[Migration history](../concepts/migration-history.md#branches).
