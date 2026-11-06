-- Renames a column and keeps its values.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v5
-- expect: columns(orders)
id | required
status | required

-- expect: SELECT status FROM orders
new
