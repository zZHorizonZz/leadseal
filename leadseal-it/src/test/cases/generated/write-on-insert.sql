-- The database writes the value of the expression into a new row, and NULL where the expression reads NULL.
-- migrate: v1
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
-- expect: SELECT total, label FROM orders ORDER BY id
1000 | GIFT
300 | NULL
