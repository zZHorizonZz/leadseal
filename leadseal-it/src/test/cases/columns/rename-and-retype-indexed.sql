-- Renames an indexed column and changes its type.
-- migrate: v21
INSERT INTO orders (id, count) VALUES (1, 7);
-- migrate: v22
-- expect: indexes(orders)
ix_orders_count | index

-- expect: SELECT total FROM orders
7
