-- Renames a commented column and adds a column with its name.
-- databases: postgresql, mariadb, sqlserver
-- migrate: v4
-- migrate: v7
-- expect: comment(orders, status)
State.

-- expect: comment(orders, state)
The new state.
