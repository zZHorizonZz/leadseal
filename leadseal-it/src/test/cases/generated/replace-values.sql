-- A column that becomes generated loses the values that it had, and gets the values of the expression.
-- migrate: v6
INSERT INTO orders (id, price, quantity, total, label) VALUES (1, 250, 4, 7, 'old');
-- migrate: v1
-- expect: SELECT total, label FROM orders
1000 | NULL
