-- An insert writes a row into another table.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'NEW');
-- expect: SELECT order_id, state, revision FROM order_events ORDER BY id
1 | NEW | NULL
