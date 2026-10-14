# Changelog

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
