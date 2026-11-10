---
description: Run protoc-gen-leadseal with protobuf-maven-plugin so that each Maven build writes the next leadseal migration.
content_type: how-to
---

# Generate migrations with protoc

`protoc-gen-leadseal` is a protoc plugin.
It reads the table messages of the files that protoc compiles, and returns the files of the next migration.
Use it when your tables are protobuf messages.
To build the schema in Java instead, refer to [Generate migrations from Java](generate-migrations-from-java.md).

## Before you begin

- Add the GitHub Packages repository and your token, as [Get started](../get-started.md) shows.
- Mark at least one message as a table.
  Refer to [Table options](../schemas/table-options.md).

## Steps

1. Add `leadseal-protobuf`, so that your protobuf files can import `leadseal/v1/options.proto`:

   ```xml
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-protobuf</artifactId>
     <version>0.3.2-SNAPSHOT</version> <!-- x-release-please-version -->
     <scope>provided</scope>
   </dependency>
   ```

2. Add `leadseal-protoc` to protobuf-maven-plugin as a `jvm-maven` plugin.
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
           <version>0.3.2-SNAPSHOT</version> <!-- x-release-please-version -->
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

3. Add a `dialect` option for each database, separated by commas:

   ```xml
   <options>migrations=${project.basedir}/src/main/resources/db/migration,dialect=postgresql,dialect=sqlite</options>
   ```

4. Build the project:

   ```bash
   mvn compile
   ```

5. Commit the migration directory.

## Check the result

The migration directory has a new folder named after the time of the build in UTC, such as `20261014104830`.
The folder has `migration.json` and one SQL file for each dialect.
When your table messages did not change, the build writes nothing.

## Run the plugin after you delete a migration

protobuf-maven-plugin skips protoc when no protobuf file changed since the last build.
Turn off its incremental compilation to run the plugin again:

```bash
mvn compile -Dprotobuf.compiler.incremental=false
```

## Add a database to an existing project

Add its `dialect` option and build.
The plugin writes the SQL of the new database for every existing migration.
When your table messages did not change, it writes no other file.

## Related

- [Protoc plugin options](protoc-plugin-options.md)
- [Apply migrations with Flyway](apply-migrations-with-flyway.md)
- [Troubleshooting migrations](troubleshooting-migrations.md)
