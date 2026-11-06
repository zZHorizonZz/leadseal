-- Widens an integer column.
-- migrate: v7
INSERT INTO orders (id, state, total) VALUES (1, 'new', 7);
-- migrate: v8
UPDATE orders SET total = total + 5000000000;
-- expect: SELECT total FROM orders
5000000007
