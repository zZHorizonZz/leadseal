-- Changes the type of two columns of one index.
-- migrate: v19
INSERT INTO orders (id, count, line) VALUES (1, 7, 2);
-- migrate: v20
-- expect: indexes(orders)
ix_orders_count_line | index

-- expect: SELECT count, line FROM orders
7 | 2
