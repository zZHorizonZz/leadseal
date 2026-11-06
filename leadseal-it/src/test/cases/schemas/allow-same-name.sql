-- Allows the same table name in two schemas.
-- databases: postgresql, mariadb, sqlserver
-- migrate: v2
INSERT INTO archive.customers (id, name) VALUES (1, 'Ada');
-- expect: SELECT COUNT(*) FROM sales.customers
0

-- expect: SELECT COUNT(*) FROM archive.customers
1
