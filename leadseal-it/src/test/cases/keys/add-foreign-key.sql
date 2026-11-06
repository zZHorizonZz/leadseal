-- Adds a foreign key to a table with rows.
-- migrate: v3
INSERT INTO customers VALUES (1);
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v4
-- expect: foreign keys(orders)
customer_id -> customers.id

-- expect: SELECT id FROM orders
1

INSERT INTO orders VALUES (2, 9, 1);
-- error:
DELETE FROM customers;
-- error:
