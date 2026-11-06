-- A statement cannot write a generated column.
-- migrate: v1
INSERT INTO orders (id, price, quantity, total) VALUES (1, 250, 4, 7);
-- error:
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
UPDATE orders SET total = 7;
-- error:
-- expect: SELECT total FROM orders ORDER BY id
1000
300
