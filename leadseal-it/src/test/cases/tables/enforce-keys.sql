-- The database enforces the unique index and the foreign key, and cascades the delete.
-- migrate: v1
INSERT INTO customers (id, email) VALUES (1, 'a@example.com');
INSERT INTO orders (id, customer_id, state) VALUES (1, 1, 'new');
INSERT INTO customers (id, email) VALUES (2, 'a@example.com');
-- error:
INSERT INTO orders (id, customer_id, state) VALUES (2, 9, 'new');
-- error:
DELETE FROM customers;
-- expect: SELECT id FROM orders
