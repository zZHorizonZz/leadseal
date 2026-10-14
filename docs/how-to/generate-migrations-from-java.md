# Generate migrations from Java

You can generate migrations without protoc. Build a `Schema` and pass it to `Migrations`.

1. Add `leadseal-dialect` and the dialect modules that you use:

   ```xml
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-dialect-postgres</artifactId>
     <version>0.1.1-SNAPSHOT</version> <!-- x-release-please-version -->
   </dependency>
   ```

2. Build the schema. A column has a type and attributes, such as `NOT NULL` or a default. A table has columns and
   attributes, such as its primary key and indexes. Give each table and column a stable id. Leadseal matches tables and
   columns by id, so a new name with the same id is a rename:

   ```java
   Column id = Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNullAttribute.INSTANCE,
       IdentityAttribute.INSTANCE);
   Column state = Column.of("2", "state", new TextType(20), NotNullAttribute.INSTANCE,
       new LiteralDefaultAttribute("new"));
   Table orders = Table.of("shop.Order", "orders", List.of(id, state),
       PrimaryKeyAttribute.of("orders", List.of("id")),
       IndexAttribute.of("orders", List.of("state"), false),
       new CommentAttribute("The orders of the shop."));
   Schema schema = Schema.of(new Namespace("sales", List.of(orders)));
   ```

   `Schema.of(orders)` puts the tables in the default namespace, the default schema of the database connection.

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

To set the clock that names migrations, or the prefix of the temporary objects of migrations, pass
`MigrationOptions.defaults().withPrefix("")` or another `MigrationOptions` as the third argument of `Migrations`.

To find a dialect by name, call `Dialect.named("postgresql")`. It finds the dialect modules on the class path or
module path with `ServiceLoader`.
