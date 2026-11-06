-- Creates an index with a descending column.
-- migrate: v12
INSERT INTO orders VALUES (1, 1, 1);
-- expect: indexes(orders)
ix_orders_customer_id_line | index
