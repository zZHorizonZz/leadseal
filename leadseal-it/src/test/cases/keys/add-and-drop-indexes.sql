-- Adds a unique and a plain index, and drops them later.
-- migrate: v1
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v9
-- expect: indexes(orders)
ix_orders_line | index
ux_orders_customer_id_line | unique

INSERT INTO orders VALUES (2, 1, 1);
-- error:
-- migrate: v1
INSERT INTO orders VALUES (2, 1, 1);
-- expect: indexes(orders)
