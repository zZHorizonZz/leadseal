# Generate migrations with protoc

`protoc-gen-leadseal` is a protoc plugin. It reads the table messages of the files that protoc compiles, and returns
the files of a new migration. protoc writes these files to the plugin output directory.

The plugin reads the migration history from the directory in its `migrations` option. Set the plugin output to the
same directory.

## Options

| Option       | Required | Description                                                                                         |
| ------------ | -------- | --------------------------------------------------------------------------------------------------- |
| `migrations` | Yes      | The migration directory                                                                             |
| `dialect`    | Yes      | A database: `postgresql`, `mariadb`, `sqlserver` or `sqlite`. Repeat for each one                   |
| `prefix`     | No       | The prefix of the temporary objects of migrations. The default is `leadseal_`. `prefix=` removes it |

Separate options with commas, for example `migrations=db/migration,dialect=postgresql,dialect=sqlite`.

## Use protobuf-maven-plugin

1. Add `leadseal-protobuf` to your dependencies, so that your protobuf files can import `leadseal/v1/options.proto`:

   ```xml
   <dependency>
     <groupId>dev.horizon</groupId>
     <artifactId>leadseal-protobuf</artifactId>
     <version>0.1.1-SNAPSHOT</version> <!-- x-release-please-version -->
     <scope>provided</scope>
   </dependency>
   ```

2. Add the plugin as a `jvm-maven` plugin:

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
           <version>0.1.1-SNAPSHOT</version> <!-- x-release-please-version -->
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

3. Run `mvn compile`.

protobuf-maven-plugin skips protoc when no protobuf file changed since the last build. Run
`mvn compile -Dprotobuf.compiler.incremental=false` to run the plugin again, for example after you delete a migration.

## Use protoc

Each release has `protoc-gen-leadseal` executables for Linux, macOS and Windows, named
`leadseal-protoc-<version>-<os>-<architecture>.exe`. They run the plugin JAR, so they need Java 25 or later.

1. Download the executable for your system and rename it to `protoc-gen-leadseal`, or `protoc-gen-leadseal.exe` on
   Windows.

2. Add `leadseal/v1/options.proto` to an import path. The file is in the `leadseal-protobuf` JAR.

3. Run protoc:

   ```sh
   protoc -I proto -I include \
     --plugin=protoc-gen-leadseal=./protoc-gen-leadseal \
     --leadseal_out=db/migration \
     --leadseal_opt=migrations=db/migration,dialect=postgresql \
     shop/v1/order.proto
   ```

## Errors

The plugin returns errors to protoc, and protoc prints them with the `--leadseal_out:` prefix. For example:

```txt
--leadseal_out: Migration generation failed: No installed dialect is named 'oracle'. Installed dialects: [mariadb, postgresql, sqlite, sqlserver].
```
