-- Moves a table to the default schema.
-- databases: postgresql, mariadb, sqlserver
-- migrate: v3
INSERT INTO crm.customers (id, name) VALUES (1, 'Ada');
-- migrate: v5
-- expect: SELECT name FROM customers
Ada
