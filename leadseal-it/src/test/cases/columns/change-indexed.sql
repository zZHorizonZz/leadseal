-- Changes an indexed column.
-- migrate: v11
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v12
-- expect: indexes(orders)
ix_orders_state | index

-- expect: SELECT state FROM orders
new
