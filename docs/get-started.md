---
description: Generate a first PostgreSQL migration with leadseal from one protobuf table message in a Maven build.
content_type: get-started
---

# Get started

You mark one protobuf message as a table, build the project, and get a PostgreSQL migration that Flyway applies.

## Prerequisites

- Java 25 or later.
- Maven.
- A GitHub account and a personal access token with the `read:packages` scope.
  GitHub Packages requires a token, also for public packages.
- PostgreSQL 13 or later, to apply the migration.

The build runs protoc 4.36.2 through protobuf-maven-plugin 5.1.11, so you do not install protoc.

## 1. Add leadseal to the project

1. Add the GitHub Packages repository to your `pom.xml`:

   ```xml
   <repositories>
     <repository>
       <id>github</id>
       <url>https://maven.pkg.github.com/zZHorizonZz/leadseal</url>
     </repository>
   </repositories>
   ```

2. Add your token to `~/.m2/settings.xml`:

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

3. Add `leadseal-protobuf`, which contains `leadseal/v1/options.proto`:

   <!-- x-release-please-start-version -->
   ```xml
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-protobuf</artifactId>
     <version>0.3.0</version>
     <scope>provided</scope>
   </dependency>
   ```
   <!-- x-release-please-end -->

## 2. Mark a message as a table

1. Create `src/main/protobuf/shop/v1/shop.proto`:

   ```proto
   syntax = "proto3";

   package shop.v1;

   import "leadseal/v1/options.proto";

   message Order {
     option (leadseal.v1.table) = {name: "orders"};

     int64 id = 1 [(leadseal.v1.column).primary_key = true];
     string state = 2 [(leadseal.v1.column) = {index: true, text: {length: 20}}];
     optional string note = 3;
   }
   ```

## 3. Run the plugin

1. Add protobuf-maven-plugin with `leadseal-protoc`.
   Set the plugin output and the `migrations` option to the same directory:

   ```xml
   <plugin>
     <groupId>io.github.ascopes</groupId>
     <artifactId>protobuf-maven-plugin</artifactId>
     <version>5.1.11</version>
     <configuration>
       <protoc>4.36.2</protoc>
       <plugins>
         <plugin kind="jvm-maven">
           <groupId>dev.horizon</groupId>
           <artifactId>leadseal-protoc</artifactId>
           <version>0.3.0</version> <!-- x-release-please-version -->
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

2. Build the project:

   ```bash
   mvn compile
   ```

## 4. Apply the migration

1. Point Flyway to the PostgreSQL folders of the migration directory:

   ```properties
   spring.flyway.locations=classpath:db/migration/**/postgresql
   ```

2. Start your application, or run Flyway, against your database.

## Result

The plugin writes the first migration.
The folder name is the time of the build in UTC:

```text
src/main/resources/db/migration/
  20261014104830/
    migration.json
    postgresql/V20261014104830.sql
  snapshots/298c2267bb456adc/contract.json
```

`V20261014104830.sql` contains:

```sql
CREATE TABLE "orders" ("id" bigint NOT NULL, "state" varchar(20) NOT NULL, "note" text, CONSTRAINT "pk_orders" PRIMARY KEY ("id"));

CREATE INDEX "ix_orders_state" ON "orders" ("state");
```

When you change the message and build again, the plugin writes a new migration with only the changes.
Commit the migration directory with your code.

## Next steps

- [Generate migrations with protoc](migrations/generate-migrations-with-protoc.md)
- [Table options](schemas/table-options.md)
- [Apply migrations with Flyway](migrations/apply-migrations-with-flyway.md)
