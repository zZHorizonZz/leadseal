-- Drops a check.
-- migrate: v2
-- migrate: v1
INSERT INTO orders (id, quantity) VALUES (1, -1);
-- expect: SELECT COUNT(*) FROM orders
1
