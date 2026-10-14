# Generate migrations from Java

You can generate migrations without protoc. Build a `Schema` and pass it to `Migrations`.

1. Add `leadseal-dialect` and the dialect modules that you use:

   ```xml
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-dialect-postgres</artifactId>
     <version>0.1.0</version> <!-- x-release-please-version -->
   </dependency>
   ```

2. Build the schema. Give each table and column a stable id. Leadseal matches tables and columns by id, so a new
   name with the same id is a rename:

   ```java
   Column id = new Column("1", "id", ColumnType.BIGINT, null, false);
   Column state = new Column("2", "state", ColumnType.TEXT, 20, false);
   Table orders = new Table("shop.Order", "orders", List.of(id, state),
       PrimaryKey.of("orders", List.of("id")),
       List.of(Index.of("orders", List.of("state"), false)),
       List.of());
   Schema schema = new Schema(List.of(orders));
   ```

   To read table messages of protobuf files, use `ProtoSchema` from `leadseal-protobuf`:

   ```java
   Schema schema = new ProtoSchema().read(List.of(OrderProto.getDescriptor()));
   ```

3. Generate the migration:

   ```java
   Migrations migrations = new Migrations(Path.of("src/main/resources/db/migration"),
       List.of(new PostgresDialect()));
   Migrations.Result result = migrations.generate(schema);
   ```

   `result.migration()` is the new migration folder, or an empty optional if the schema did not change.
   `result.files()` lists the written files.

To get the files without writing them, call `migrations.plan(schema)`. It returns the same result, and you write each
`MigrationFile` yourself.

To find a dialect by name, call `Dialect.named("postgresql")`. It finds the dialect modules on the class path or
module path with `ServiceLoader`.
