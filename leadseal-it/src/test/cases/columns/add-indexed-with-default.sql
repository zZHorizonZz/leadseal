-- Adds an indexed column with a default to a table with rows.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v18
UPDATE orders SET total = total + 1;
-- expect: SELECT total FROM orders
6

-- expect: indexes(orders)
ix_orders_total | index
