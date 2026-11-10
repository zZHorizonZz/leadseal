# Changelog

## [0.3.1](https://github.com/zZHorizonZz/leadseal/compare/v0.3.0...v0.3.1) (2026-11-10)


### Fixes

* give the current time default of SQLite six digits of fraction, so that it compares as text with the times of clients. ([#25](https://github.com/zZHorizonZz/leadseal/issues/25)) ([d7c9567](https://github.com/zZHorizonZz/leadseal/commit/d7c9567b750c4f6bf6fab2b2d8a97f2950665134))

## [0.3.0](https://github.com/zZHorizonZz/leadseal/compare/v0.2.0...v0.3.0) (2026-11-06)


### ⚠ BREAKING CHANGES

* ColumnAttribute permits Generated, TableChange permits DropGeneratedColumn, and Change.Phase has DROP_GENERATED_COLUMNS and ADD_GENERATED_COLUMNS.
* Ddl has the new method trigger(), which each dialect implements with a TriggerDdl. TableAttribute permits Triggers, SqlExpression and Value permit ForeignColumn, Change.Phase has DROP_TRIGGERS and ADD_TRIGGERS, and TableAttribute has validate(Table, Schema), which ForeignKey implements.
* a type change to a type that holds fewer values fails unless the options allow unsafe changes. MigrationOptions has the new component unsafe, Script has the new component warnings, and MigrationPlan has the new method warnings().
* Change has no table() any more; a change of a table is a TableChange. An enum field is an enum column instead of a text column, so the first migration after the upgrade changes the type of each enum column.
* split the model into packages, give each dialect a ddl, and rewrite the docs. ([#16](https://github.com/zZHorizonZz/leadseal/issues/16))

### Features

* add generated columns, which the database writes from an expression of the row. ([#22](https://github.com/zZHorizonZz/leadseal/issues/22)) ([271448c](https://github.com/zZHorizonZz/leadseal/commit/271448ced969e391760d4026038888bccdd10cac))
* add rewrites and triggers, written as the triggers of each database. ([#21](https://github.com/zZHorizonZz/leadseal/issues/21)) ([caca0b7](https://github.com/zZHorizonZz/leadseal/commit/caca0b7a2bb3c26c8c948e7b62ea3a9b49b2b113))
* store enum columns as enum types, native on postgresql and mariadb. ([#19](https://github.com/zZHorizonZz/leadseal/issues/19)) ([e1d19cd](https://github.com/zZHorizonZz/leadseal/commit/e1d19cde6f58d4dab2f26d1def05ee9f2e988a8b))
* warn about drops, and refuse a type change that can lose values unless allow_unsafe is set. ([#20](https://github.com/zZHorizonZz/leadseal/issues/20)) ([7376014](https://github.com/zZHorizonZz/leadseal/commit/73760144e91c2b3608a309a94dee8362e610519e))


### Refactoring

* split the model into packages, give each dialect a ddl, and rewrite the docs. ([#16](https://github.com/zZHorizonZz/leadseal/issues/16)) ([5ea8874](https://github.com/zZHorizonZz/leadseal/commit/5ea8874c14d14b5365e16ff3e7fe2846c703dd88))

## [0.2.0](https://github.com/zZHorizonZz/leadseal/compare/v0.1.0...v0.2.0) (2026-10-14)


### ⚠ BREAKING CHANGES

* a migration directory with named folders fails with "Rename the folder to <version>"; rename each folder and its SQL files.
* releases no longer include leadseal-protoc-*.exe executables. Use protobuf-maven-plugin with a jvm-maven plugin.

### Features

* name migration folders and SQL files by their version alone ([b4df696](https://github.com/zZHorizonZz/leadseal/commit/b4df696c75b0745e229b098a52131a0588e55bd8))


### Build

* drop Canteen executables and run the plugin through protobuf-maven-plugin ([8d2c802](https://github.com/zZHorizonZz/leadseal/commit/8d2c802bf0828f84c6975c11ea7de8d391690389))

## 0.1.0 (2026-10-14)


### Features

* generate SQL migrations from protobuf messages for PostgreSQL, MariaDB, SQL Server and SQLite ([a40bcb6](https://github.com/zZHorizonZz/leadseal/commit/a40bcb6260aaad6f671e64ef662301159b157c1d))
