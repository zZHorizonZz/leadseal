-- Changes the default.
-- migrate: v5
INSERT INTO orders (id) VALUES (1);
-- migrate: v6
INSERT INTO orders (id) VALUES (2);
-- expect: SELECT total FROM orders ORDER BY id
1
2
