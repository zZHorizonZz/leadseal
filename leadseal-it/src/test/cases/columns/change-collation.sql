-- Changes the collation of an indexed column.
-- migrate: v16
INSERT INTO orders (id, code) VALUES (1, 'a');
-- migrate: v17
-- expect: SELECT COUNT(*) FROM orders WHERE code = 'A'
0

-- expect: indexes(orders)
ix_orders_code | index
