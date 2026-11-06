-- Renames a table and keeps its rows, its key and its index.
-- migrate: v2
INSERT INTO customers (id, email) VALUES (1, 'a@example.com');
-- migrate: v3
-- expect: columns(customers)

-- expect: SELECT email FROM clients
a@example.com

-- expect: primary key(clients)
id

-- expect: indexes(clients)
ux_clients_email | unique
