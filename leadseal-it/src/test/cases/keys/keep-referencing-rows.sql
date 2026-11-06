-- Keeps the rows that reference a table that a migration changes.
-- migrate: v10
INSERT INTO customers VALUES (1, 'Ada');
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v11
-- expect: SELECT id FROM orders
1

-- expect: foreign keys(orders)
customer_id -> customers.id
