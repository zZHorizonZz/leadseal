-- Renames a table in a schema, with a foreign key to it.
-- databases: postgresql, mariadb, sqlserver
-- migrate: v6
INSERT INTO crm.customers (id, name) VALUES (1, 'Ada');
-- migrate: v7
-- expect: SELECT name FROM crm.clients
Ada

INSERT INTO crm.orders (id, customer_id) VALUES (1, 1);
