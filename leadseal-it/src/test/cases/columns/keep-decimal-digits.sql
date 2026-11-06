-- Keeps the digits of a decimal.
-- migrate: v13
INSERT INTO orders (id, price) VALUES (1, '1234567890.12');
-- expect: SELECT price FROM orders
1234567890.12
