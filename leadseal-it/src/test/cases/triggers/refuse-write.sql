-- An assert refuses an insert and an update, and the rows stay as they were.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'NEW');
INSERT INTO orders (id, state, total) VALUES (2, 'NEW', -1);
-- error: The total is negative.
UPDATE orders SET total = -1;
-- error: The total is negative.
-- expect: SELECT id, total FROM orders
1 | 0
