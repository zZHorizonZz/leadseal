-- Drops a column.
-- migrate: v4
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v1
-- expect: columns(orders)
id | required
state | required

-- expect: SELECT state FROM orders
new
