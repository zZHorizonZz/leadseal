-- Creates tables in schemas, with a foreign key from one schema to another.
-- databases: postgresql, mariadb, sqlserver
-- migrate: v1
INSERT INTO crm.customers (id, name) VALUES (1, 'Ada');
INSERT INTO sales.orders (id, customer_id) VALUES (1, 1);
INSERT INTO sales.orders (id, customer_id) VALUES (2, 9);
-- error:
DELETE FROM crm.customers;
-- expect: SELECT COUNT(*) FROM sales.orders
0
