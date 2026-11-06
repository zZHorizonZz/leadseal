-- The triggers keep writing after a migration renames the table that they write into.
-- migrate: v1
-- migrate: v2
INSERT INTO orders (id, state) VALUES (1, 'NEW');
-- expect: SELECT order_id, state, revision FROM events
1 | NEW | NULL
