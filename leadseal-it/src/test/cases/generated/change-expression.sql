-- A migration that changes the expression writes the new values, and keeps the other columns.
-- migrate: v1
INSERT INTO orders (id, price, quantity, note) VALUES (1, 250, 4, 'gift');
INSERT INTO orders (id, price, quantity, note) VALUES (2, 100, 3, NULL);
-- migrate: v3
-- expect: SELECT price, quantity, total, label FROM orders ORDER BY id
250 | 4 | 1001 | GIFT
100 | 3 | 301 | NULL
