-- A migration drops generated columns, and keeps the columns that they read.
-- migrate: v1
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
-- migrate: v2
-- expect: columns(orders)
id | required
price | required
quantity | required
note | optional

-- expect: SELECT price, quantity FROM orders ORDER BY id
250 | 4
100 | 3
