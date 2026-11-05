---
description: The options of protoc-gen-leadseal, the leadseal protoc plugin, as a protoc parameter or as command line arguments.
content_type: reference
---

# Protoc plugin options

`protoc-gen-leadseal` takes its options as a protoc parameter, for example `--leadseal_opt` or the `<options>` of protobuf-maven-plugin.
Refer to [Generate migrations with protoc](generate-migrations-with-protoc.md).

Separate options with commas: `migrations=db/migration,dialect=postgresql,dialect=sqlite`.

## Parameter options

| Name         | Type             | Default            | Description                                                                                        |
| ------------ | ---------------- | ------------------ | -------------------------------------------------------------------------------------------------- |
| `migrations` | path             | none, required     | The migration directory. Set the plugin output to the same directory.                              |
| `dialect`    | string, repeated | none, at least one | A database: `postgresql`, `mariadb`, `sqlserver` or `sqlite`. Repeat the option for each database. |
| `prefix`     | string           | `leadseal_`        | The prefix of the temporary objects of migrations. `prefix=` sets no prefix.                       |

An option without a value, or an option with another name, stops the plugin with an error.
When an option appears twice, the last value counts, except for `dialect`.

## Command line arguments

When you run the plugin yourself, it also takes command line arguments.
A run takes either a protoc parameter or command line arguments, not both.

| Name           | Type             | Default            | Description                                                                       |
| -------------- | ---------------- | ------------------ | --------------------------------------------------------------------------------- |
| `--migrations` | path             | none, required     | The migration directory.                                                          |
| `--dialect`    | string, repeated | none, at least one | A database. Repeat the argument for each database.                                |
| `--prefix`     | string           | `leadseal_`        | The prefix of the temporary objects of migrations. An empty value sets no prefix. |
| `--help`       | flag             | off                | Prints the arguments.                                                             |

## Temporary objects

Migrations name their temporary objects with the prefix.

| Object                                              | Database   |
| --------------------------------------------------- | ---------- |
| The temporary table of a table rebuild              | SQLite     |
| The table that checks foreign keys during a rebuild | SQLite     |
| The variables of statements                         | SQL Server |
