-- Adds a check to a table with rows.
-- migrate: v1
INSERT INTO orders (id, quantity) VALUES (1, 5);
-- migrate: v2
INSERT INTO orders (id, quantity) VALUES (2, -1);
-- error:
-- expect: SELECT quantity FROM orders
5
