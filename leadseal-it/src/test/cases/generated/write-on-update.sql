-- An update of a column that the expression reads writes the value again.
-- migrate: v1
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
UPDATE orders SET quantity = 5, note = 'box' WHERE id = 2;
-- expect: SELECT total, label FROM orders ORDER BY id
1000 | GIFT
500 | BOX
