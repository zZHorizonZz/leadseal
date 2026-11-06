-- Applies the delete action set null.
-- migrate: v8
INSERT INTO customers VALUES (1);
INSERT INTO orders VALUES (1, 1, 1);
DELETE FROM customers;

-- expect: SELECT customer_id FROM orders
NULL
