-- Adds an optional column.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v4
-- expect: SELECT state, note FROM orders
new | NULL
