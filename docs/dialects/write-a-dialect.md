---
description: Write a leadseal dialect for another database, register it for ServiceLoader, and test it with the dialect contract tests.
content_type: how-to
---

# Write a dialect

A dialect is a `Dialect` that writes names, values and types, and a `Ddl` that gives the commands of each change.
The default methods of `Ddl` write standard SQL, so a dialect overrides only what its database writes differently.
Use this page to support a database that leadseal has no module for.

## Before you begin

- Read [About database differences](about-database-differences.md) to see what a dialect decides.
- Have Docker running, so that the contract tests can start the database with Testcontainers.

## Steps

1. Create a module that depends on `leadseal-dialect`, and on its test-jar for the contract tests:

   ```xml
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-dialect</artifactId>
     <version>0.2.1-SNAPSHOT</version> <!-- x-release-please-version -->
   </dependency>
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-dialect</artifactId>
     <version>0.2.1-SNAPSHOT</version> <!-- x-release-please-version -->
     <type>test-jar</type>
     <scope>test</scope>
   </dependency>
   ```

2. Implement `Dialect`.
   It names the dialect, quotes identifiers and text, and gives the SQL of types, literals and generated defaults:

   ```java
   public final class PostgresDialect implements Dialect {

     public static final String NAME = "postgresql";
     private static final DecimalLimit DECIMAL = new DecimalLimit("PostgreSQL", 1000, 1000);

     private final Ddl ddl = new PostgresDdl(this);

     @Override
     public String name() {
       return NAME;
     }

     @Override
     public String quote(String identifier) {
       return "\"" + identifier.replace("\"", "\"\"") + "\"";
     }

     @Override
     public String string(String text) {
       return "'" + text.replace("'", "''") + "'";
     }
   ```

   Also implement `type`, `literal`, `currentDate`, `currentTimestamp`, `randomUuid` and `ddl`.

   From `leadseal-dialect-postgres/src/main/java/dev/horizon/leadseal/postgres/PostgresDialect.java`.

3. Implement `Ddl` with its four parts: tables, columns, constraints and comments:

   ```java
   record PostgresDdl(Dialect dialect) implements Ddl {

     @Override
     public TableDdl table() {
       return new PostgresTableDdl(this);
     }

     @Override
     public ColumnDdl column() {
       return new PostgresColumnDdl(this);
     }

     @Override
     public ConstraintDdl constraint() {
       return () -> this;
     }

     @Override
     public CommentDdl comment() {
       return new PostgresCommentDdl(dialect);
     }
   }
   ```

   `TableDdl` needs `moveTable`, `ColumnDdl` needs `collate`, and `CommentDdl` needs `commentColumn` and `commentTable`.
   Every other method has a default.

4. Refuse a change that the database cannot make with an `UnsupportedChangeException`:

   ```java
   @Override
   public List<Command> createSchema(String namespace) {
     throw new UnsupportedChangeException("SQLite has no namespaces, so it cannot create the namespace '" + namespace + "'.");
   }
   ```

   From `leadseal-dialect-sqlite/src/main/java/dev/horizon/leadseal/sqlite/SqliteTableDdl.java`.

5. Return no commands for a change that the database has nothing to write for:

   ```java
   record SqliteCommentDdl() implements CommentDdl {

     @Override
     public List<Command> commentColumn(ColumnTransition column) {
       return List.of();
     }

     @Override
     public List<Command> commentTable(TableTransition table) {
       return List.of();
     }
   }
   ```

6. Register the dialect in `module-info.java`:

   ```java
   module dev.horizon.leadseal.postgres {
     requires transitive dev.horizon.leadseal;

     exports dev.horizon.leadseal.postgres;

     provides dev.horizon.leadseal.Dialect with dev.horizon.leadseal.postgres.PostgresDialect;
   }
   ```

7. Register it for the class path in `src/main/resources/META-INF/services/dev.horizon.leadseal.Dialect`:

   ```text
   dev.horizon.leadseal.postgres.PostgresDialect
   ```

8. Write the test database.
   `ContainerDatabase` starts the container once and creates one database for each test:

   ```java
   final class PostgresTestDatabase extends ContainerDatabase {

     private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
         System.getProperty("leadseal.test.postgres.image", "postgres:17-alpine"));

     PostgresTestDatabase() {
       super(CONTAINER);
     }
   ```

   Also implement `dialect`, `binaryCollation`, `flywayParser` and `trigger`.

9. Run the contract tests.
   Extend `DialectContract`, and add the capability contracts that the database has:

   ```java
   class PostgresContractTest extends DialectContract {

     PostgresContractTest() {
       super(new PostgresTestDatabase());
     }

     @Nested
     class Schemas extends SchemaContractTest {

       @Override
       protected TestDatabase database() {
         return fixture;
       }
     }
   ```

   The capability contracts are `SchemaContractTest`, `CommentContractTest`, `PartialIndexContractTest`, `IdentityChangeContractTest` and `IdentityRemovalContractTest`.

## Check the result

`mvn verify` runs every contract test against the database, and all of them pass.
`Dialect.of` finds the dialect by its name.

## Test the SQL of expressions

Extend `DialectExpressionTest` and give the SQL of each `Example` as a table.
A dialect that leaves out an example fails.

## Related

- [Dialect support](dialect-support.md)
- [Exceptions](../migrations/exceptions.md)
