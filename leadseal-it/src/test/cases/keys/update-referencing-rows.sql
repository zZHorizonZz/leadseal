-- Updates the rows that reference a changed key.
-- migrate: v13
INSERT INTO customers VALUES (1);
INSERT INTO orders VALUES (1, 1, 1);
UPDATE customers SET id = 2;
-- expect: SELECT customer_id FROM orders
2
