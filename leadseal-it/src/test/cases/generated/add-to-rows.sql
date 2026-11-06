-- A generated column that a migration adds gets the values of the rows that the table has.
-- migrate: v2
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
-- migrate: v1
-- expect: SELECT total, label FROM orders ORDER BY id
1000 | GIFT
300 | NULL
