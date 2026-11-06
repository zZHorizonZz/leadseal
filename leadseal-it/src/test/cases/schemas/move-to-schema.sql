-- Moves a table to another schema and keeps its rows.
-- databases: postgresql, mariadb, sqlserver
-- migrate: v3
INSERT INTO crm.customers (id, name) VALUES (1, 'Ada');
-- migrate: v4
-- expect: SELECT name FROM archive.customers
Ada

SELECT name FROM crm.customers;
-- error:
