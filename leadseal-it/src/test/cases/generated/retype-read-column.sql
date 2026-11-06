-- A migration that changes the type of a column that the expression reads keeps the values.
-- migrate: v1
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
-- migrate: v5
UPDATE orders SET quantity = 5000000000 WHERE id = 2;
-- expect: SELECT total FROM orders ORDER BY id
1000
500000000000
