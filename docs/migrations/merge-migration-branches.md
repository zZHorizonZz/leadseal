---
description: Merge two branches that each added a leadseal migration, so the migrations form one chain again.
content_type: how-to
---

# Merge migration branches

Two branches can each add a migration on top of the same schema.
After the merge, the directory has two migrations that start from the same schema, and leadseal stops with an error.
Follow these steps to put one migration after the other.

> [!WARNING]
> Delete a migration only if no shared database ran it.
> A database that ran it already has its changes, and the generated migration would make them again.

## Steps

1. Merge the branches.

2. Build the project.
   Leadseal stops with an error that names both migrations:

   ```text
   The migration 20261014070002 starts from the schema 298c2267bb456adc, but the migration before it, 20261014070001, creates the schema e1ef0ebd6b8b8350. Delete the later migration and generate it again.
   ```

3. Delete the folder of the later migration, `20261014070002` in the example.

4. Delete its snapshot: the `snapshots/<hash>` folder that only the deleted migration created.

5. Build the project again.
   With protobuf-maven-plugin, run `mvn compile -Dprotobuf.compiler.incremental=false`.

## Check the result

The directory has a new migration after the other one.
It has the changes of the deleted migration, on top of the schema of the other one.

## Let Flyway run an earlier migration

Two branches create migrations with different times.
After a merge, a database can have run the later migration of one branch but not the earlier one of the other.
To let Flyway run the earlier migration, turn on `outOfOrder`:

```properties
spring.flyway.out-of-order=true
```

## Related

- [About migration history](about-migration-history.md)
- [Troubleshooting migrations](troubleshooting-migrations.md)
