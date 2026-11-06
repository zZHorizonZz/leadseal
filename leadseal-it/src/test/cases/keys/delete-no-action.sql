-- Applies the delete action no action.
-- migrate: v5
INSERT INTO customers VALUES (1);
INSERT INTO orders VALUES (1, 1, 1);
DELETE FROM customers;
-- error:
-- expect: SELECT customer_id FROM orders
1
