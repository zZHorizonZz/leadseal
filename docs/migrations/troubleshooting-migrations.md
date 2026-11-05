---
description: Find the cause and the fix of leadseal errors about the migration history and the protoc plugin options.
content_type: troubleshooting
---

# Troubleshooting migrations

The protoc plugin returns errors to protoc, and protoc prints them after `--leadseal_out:`.
From Java, the same message is the message of a `BrokenHistoryException` or a `ConfigurationException`.
For errors about tables and columns, refer to [Troubleshooting schemas](../schemas/troubleshooting-schemas.md).

## The migration ... starts from the schema ..., but the migration before ...

```text
--leadseal_out: Migration generation failed: The migration 20261014070002 starts from the schema 298c2267bb456adc, but the migration before it, 20261014070001, creates the schema e1ef0ebd6b8b8350. Delete the later migration and generate it again.
```

**Cause**: Two branches each added a migration on top of the same schema.

**Fix**: Delete the later migration and its snapshot, and build again.
Refer to [Merge migration branches](merge-migration-branches.md).

**Verify**: The build writes a new migration after the other one, and no error.

## The option allow_unsafe is true or false, but is ...

```text
--leadseal_out: Migration generation failed: The option allow_unsafe is true or false, but is 'yes'.
```

**Cause**: The plugin option `allow_unsafe` has a value other than `true` or `false`.

**Fix**: Set `allow_unsafe=true`, or remove the option.
Refer to [Protoc plugin options](protoc-plugin-options.md).

**Verify**: The build writes no error.

## No installed dialect is named ...

```text
--leadseal_out: Migration generation failed: No installed dialect is named 'oracle'. Installed dialects: [mariadb, postgresql, sqlite, sqlserver].
```

**Cause**: A `dialect` option names a database that leadseal does not support, or the dialect module is not on the class path.

**Fix**: Use one of the installed names.
From Java, add the dialect module to your dependencies.

**Verify**: The build writes the SQL folder of each dialect.

## The migration ... starts from the schema ..., which no migration before it creates

```text
--leadseal_out: Migration generation failed: The migration 20261014070000 starts from the schema 298c2267bb456adc, which no migration before it creates.
```

**Cause**: The first migration of the directory starts from a schema other than the empty one, usually because an earlier migration folder was deleted.

**Fix**: Restore the deleted migration from version control.

**Verify**: The build writes no error.

## The migration folder ... has the migration ...

```text
--leadseal_out: Migration generation failed: The migration folder 20261014070000-orders has the migration 20261014070000. Rename the folder to 20261014070000.
```

**Cause**: A migration folder was renamed.
The folder name must be the version in its `migration.json`.

**Fix**: Rename the folder to the name that the error gives.

**Verify**: The build writes no error, and Flyway finds the same versions.

## The snapshot ... is missing.

```text
--leadseal_out: Migration generation failed: The snapshot db/migration/snapshots/298c2267bb456adc/contract.json is missing.
```

**Cause**: A snapshot folder was deleted or not committed.

**Fix**: Restore the snapshot from version control, or commit it from the branch that wrote it.

**Verify**: The build writes no error.

## The snapshot ... has the format version ...

```text
--leadseal_out: Migration generation failed: The snapshot db/migration/snapshots/298c2267bb456adc/contract.json has the format version 2, but this version of leadseal reads version 1.
```

**Cause**: A newer version of leadseal wrote the snapshot.

**Fix**: Use the leadseal version that wrote the migration directory, or a later one.

**Verify**: The build writes no error.

## Cannot read ...

```text
--leadseal_out: Migration generation failed: Cannot read db/migration/20261014070000/migration.json: Unexpected end-of-input
```

**Cause**: A `migration.json` or a snapshot is not valid JSON, for example after a merge conflict.

**Fix**: Restore the file from version control, or settle the merge conflict in it.

**Verify**: The build writes no error.

## Set the migrations option to the migration directory ...

```text
--leadseal_out: Migration generation failed: Set the migrations option to the migration directory, for example migrations=src/main/resources/db/migration.
```

**Cause**: The plugin options have no `migrations` option.

**Fix**: Add `migrations=` with the path of the plugin output.
Refer to [Protoc plugin options](protoc-plugin-options.md).

**Verify**: The build writes the migration into that directory.

## Set at least one dialect option ...

```text
--leadseal_out: Migration generation failed: Set at least one dialect option, for example dialect=postgresql.
```

**Cause**: The plugin options have no `dialect` option.

**Fix**: Add a `dialect` option for each database.

**Verify**: The build writes the SQL folder of each dialect.

## Unknown option ...

```text
--leadseal_out: Migration generation failed: Unknown option 'dialects'. The options are migrations, dialect and prefix.
```

**Cause**: An option name is misspelled.

**Fix**: Use `migrations`, `dialect` or `prefix`.

**Verify**: The build writes no error.

## The option ... needs a value ...

```text
--leadseal_out: Migration generation failed: The option 'dialect' needs a value, for example dialect=...
```

**Cause**: An option has no `=` and no value.

**Fix**: Give the option a value, such as `dialect=postgresql`.

**Verify**: The build writes no error.
