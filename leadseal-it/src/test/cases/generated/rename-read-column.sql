-- A migration that renames a column that the expression reads keeps the values.
-- migrate: v1
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
-- migrate: v4
UPDATE orders SET amount = 2 WHERE id = 1;
-- expect: SELECT amount, total FROM orders ORDER BY id
2 | 500
3 | 300
