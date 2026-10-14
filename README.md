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

1. Add the GitHub Packages repository and `leadseal-protobuf` to your `pom.xml`. The module contains
   `leadseal/v1/options.proto`:

   <!-- x-release-please-start-version -->
   ```xml
   <repositories>
     <repository>
       <id>github</id>
       <url>https://maven.pkg.github.com/zZHorizonZz/leadseal</url>
     </repository>
   </repositories>

   <dependencies>
     <dependency>
       <groupId>dev.horizon</groupId>
       <artifactId>leadseal-protobuf</artifactId>
       <version>0.1.0</version>
       <scope>provided</scope>
     </dependency>
   </dependencies>
   ```
   <!-- x-release-please-end -->

2. GitHub Packages requires a token, also for public packages. Create a personal access token with the
   `read:packages` scope, and add it to `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>github</id>
         <username>YOUR_GITHUB_USERNAME</username>
         <password>YOUR_TOKEN</password>
       </server>
     </servers>
   </settings>
   ```

3. Mark a message as a table and set its primary key:

   ```proto
   syntax = "proto3";

   package shop.v1;

   import "leadseal/v1/options.proto";

   message Order {
     option (leadseal.v1.table) = {name: "orders"};

     int64 id = 1 [(leadseal.v1.column).primary_key = true];
     string state = 2 [(leadseal.v1.column) = {length: 20, index: true}];
     optional string note = 3;
   }
   ```

4. Run the plugin with protobuf-maven-plugin. Set the plugin output and the `migrations` option to the same
   directory, and add a `dialect` option for each database:

   <!-- x-release-please-start-version -->
   ```xml
   <plugin>
     <groupId>io.github.ascopes</groupId>
     <artifactId>protobuf-maven-plugin</artifactId>
     <version>0.1.0</version>
     <configuration>
       <protoc>0.1.0</protoc>
       <plugins>
         <plugin kind="jvm-maven">
           <groupId>dev.horizon</groupId>
           <artifactId>leadseal-protoc</artifactId>
           <version>0.1.0</version>
           <options>migrations=${project.basedir}/src/main/resources/db/migration,dialect=postgresql</options>
           <outputDirectory>${project.basedir}/src/main/resources/db/migration</outputDirectory>
           <registerAsCompilationRoot>false</registerAsCompilationRoot>
         </plugin>
       </plugins>
     </configuration>
     <executions>
       <execution>
         <goals>
           <goal>generate</goal>
         </goals>
       </execution>
     </executions>
   </plugin>
   ```
   <!-- x-release-please-end -->

5. Build the project:

   ```sh
   mvn compile
   ```

   The plugin writes the first migration. The folder name starts with the time of the build, in UTC:

   ```txt
   src/main/resources/db/migration/
     20261014T083047_create_orders/
       migration.json
       ops.json
       postgresql/V20261014083047__create_orders.sql
     snapshots/3fb5dee7cb41bf96/schema.json
   ```

   `V20261014083047__create_orders.sql` contains:

   ```sql
   CREATE TABLE "orders" ("id" bigint NOT NULL, "state" varchar(20) NOT NULL, "note" text, CONSTRAINT "pk_orders" PRIMARY KEY ("id"));

   CREATE INDEX "ix_orders_state" ON "orders" ("state");
   ```

6. Point Flyway to the folders of your database:

   ```properties
   spring.flyway.locations=classpath:db/migration/**/postgresql
   ```

When you change a table message and build again, the plugin writes a new migration with only the changes. When
nothing changed, it writes nothing. Commit the migration directory to version control.

## Modules

| Module                       | Package                          | Contents                                                           |
| ---------------------------- | -------------------------------- | ------------------------------------------------------------------ |
| `leadseal-dialect`           | `dev.horizon.leadseal`           | The schema model, the diff, the migration history and the DDL tree |
| `leadseal-dialect-postgres`  | `dev.horizon.leadseal.postgres`  | `PostgresDialect`                                                  |
| `leadseal-dialect-mariadb`   | `dev.horizon.leadseal.mariadb`   | `MariaDbDialect`                                                   |
| `leadseal-dialect-sqlserver` | `dev.horizon.leadseal.sqlserver` | `SqlServerDialect`                                                 |
| `leadseal-dialect-sqlite`    | `dev.horizon.leadseal.sqlite`    | `SqliteDialect`                                                    |
| `leadseal-protobuf`          | `dev.horizon.leadseal.protobuf`  | `leadseal/v1/options.proto` and `ProtoSchema`                      |
| `leadseal-protoc`            | `dev.horizon.leadseal.protoc`    | `protoc-gen-leadseal`, as a JAR and as executables for protoc      |
| `leadseal-it`                | `dev.horizon.leadseal.it`        | Tests that run the plugin through protoc and apply with Flyway     |

## Documentation

- Concept: [Migration history](docs/concepts/migration-history.md)
- How to: [Generate migrations with protoc](docs/how-to/generate-migrations-with-protoc.md)
- How to: [Apply migrations with Flyway](docs/how-to/apply-migrations-with-flyway.md)
- How to: [Generate migrations from Java](docs/how-to/generate-migrations-from-java.md)
- Reference: [Protobuf options](docs/reference/protobuf-options.md)
- Reference: [Column types](docs/reference/column-types.md)
- Reference: [Dialects](docs/reference/dialects.md)

## Run the tests

Each dialect module runs the contract tests of `leadseal-dialect` against its database. The contract tests generate
migrations step by step, apply them with Flyway and check the tables through JDBC. `leadseal-it` runs
`protoc-gen-leadseal` through protoc on a schema that changes in three steps, and applies the migrations to every
database. Testcontainers starts PostgreSQL, MariaDB and SQL Server in Docker, and SQLite runs in process. Start
Docker, then run:

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

## License

Apache License 2.0. See [LICENSE](LICENSE).
