-- The triggers keep working after a migration changes a table that they write into.
-- migrate: v1
-- migrate: v3
INSERT INTO orders (id, state) VALUES (1, 'NEW');
-- expect: SELECT order_id, state, revision FROM order_events
1 | NEW | NULL
