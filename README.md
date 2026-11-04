# Leadseal

Leadseal generates SQL migration files from protobuf messages. You mark messages as tables with protobuf options.
When protoc runs, the leadseal plugin compares the messages with the schema of the latest migration and writes a new
migration for each database that you choose. You apply the migrations with Flyway or another migration tool.
Leadseal supports PostgreSQL, MariaDB, SQL Server and SQLite.

## Requirements

- Java 25 or later.
- protoc 4.x, for example through [protobuf-maven-plugin](https://ascopes.github.io/protobuf-maven-plugin/).
- One of these databases:

  | Database   | Minimum version |
  | ---------- | --------------- |
  | PostgreSQL | 13              |
  | MariaDB    | 11.4            |
  | SQL Server | 2017            |
  | SQLite     | 3.35            |

## Get started

Follow [Get started](docs/get-started.md) to generate a first PostgreSQL migration from one table message.
The [documentation](docs/README.md) covers every option, dialect and error.

## Modules

| Module | Package | Contents |
| --- | --- | --- |
| `leadseal-dialect` | `dev.horizon.leadseal` | The schema model, `Dialect`, `ScriptWriter` and the exceptions |
| `leadseal-dialect` | `dev.horizon.leadseal.attribute` | The column and table attributes, such as `Index` and `ForeignKey` |
| `leadseal-dialect` | `dev.horizon.leadseal.change` | The changes between two schemas, such as `AddColumn` |
| `leadseal-dialect` | `dev.horizon.leadseal.command` | The `Ddl` of a dialect, its commands and clauses |
| `leadseal-dialect` | `dev.horizon.leadseal.expression` | The SQL expressions of checks and triggers, such as `Comparison` |
| `leadseal-dialect` | `dev.horizon.leadseal.migration` | `MigrationDirectory`, `Plan` and `MigrationOptions` |
| `leadseal-dialect` | `dev.horizon.leadseal.type` | The column types, such as `TextType` and `IntegerType` |
| `leadseal-dialect-postgres` | `dev.horizon.leadseal.postgres` | `PostgresDialect` |
| `leadseal-dialect-mariadb` | `dev.horizon.leadseal.mariadb` | `MariaDbDialect` |
| `leadseal-dialect-sqlserver` | `dev.horizon.leadseal.sqlserver` | `SqlServerDialect` |
| `leadseal-dialect-sqlite` | `dev.horizon.leadseal.sqlite` | `SqliteDialect` |
| `leadseal-protobuf` | `dev.horizon.leadseal.protobuf` | `leadseal/v1/options.proto` and `ProtoSchema` |
| `leadseal-protoc` | `dev.horizon.leadseal.protoc` | `protoc-gen-leadseal`, as a shaded JAR |
| `leadseal-it` | `dev.horizon.leadseal.it` | Tests that run the plugin in a build and apply the migrations with Flyway |

## Documentation

- [Leadseal](docs/README.md): the overview and the map of the documentation.
- [Get started](docs/get-started.md)
- [Migrations](docs/migrations/README.md): generate, apply and merge migrations.
- [Schemas](docs/schemas/README.md): the protobuf options and the column types.
- [Dialects](docs/dialects/README.md): what each database supports, and how to write a dialect.

## Run the tests

Each dialect module runs the contract tests of `leadseal-dialect` against its database. The contract tests generate
migrations step by step, apply them with Flyway and check the tables through JDBC. `leadseal-it` runs
`protoc-gen-leadseal` through protobuf-maven-plugin on a schema that changes in three steps, and applies the
migrations to every database. Testcontainers starts PostgreSQL, MariaDB and SQL Server in Docker, and SQLite runs in process.
Start Docker, then run:

```sh
mvn verify
```

To test another server version, set its image with a system property:

| Property                        | Default                                      |
| ------------------------------- | -------------------------------------------- |
| `leadseal.test.postgres.image`  | `postgres:17-alpine`                         |
| `leadseal.test.mariadb.image`   | `mariadb:13.0`                               |
| `leadseal.test.sqlserver.image` | `mcr.microsoft.com/mssql/server:2022-latest` |

For example: `mvn verify -Dleadseal.test.mariadb.image=mariadb:11.4`.

## Resources

Leadseal uses ideas and formats from these projects and documents:

- [Prisma Migrate](https://www.prisma.io/docs/orm/prisma-migrate): the folder for each migration, named with its time
  and a description, and turning off foreign keys while SQLite rebuilds a table.
- [SQLite ALTER TABLE](https://www.sqlite.org/lang_altertable.html#otheralter): the steps that rebuild a table for
  changes that SQLite cannot make in place.
- [Flyway](https://documentation.red-gate.com/fd): the SQL file names, wildcard locations and script configuration
  files.
- [protoc plugin protocol](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/compiler/plugin.proto):
  the request and response that protoc exchanges with `protoc-gen-leadseal`.
- [Vert.x gRPC protoc plugin](https://github.com/eclipse-vertx/vertx-grpc/tree/main/vertx-grpc-protoc-plugin2): the
  structure of `protoc-gen-leadseal` and its shaded JAR.
- [protobuf-maven-plugin](https://ascopes.github.io/protobuf-maven-plugin/): runs protoc and the plugin in Maven
  builds.

## License

Apache License 2.0. See [LICENSE](LICENSE).
