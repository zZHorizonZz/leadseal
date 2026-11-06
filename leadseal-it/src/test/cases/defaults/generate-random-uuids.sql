-- Generates a random UUID for each row.
-- migrate: v1
INSERT INTO orders (id) VALUES (1);
-- migrate: v4
INSERT INTO orders (id) VALUES (2);
INSERT INTO orders (id) VALUES (3);
-- expect: SELECT COUNT(DISTINCT token) FROM orders
3
