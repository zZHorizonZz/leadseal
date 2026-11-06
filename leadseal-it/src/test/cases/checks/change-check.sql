-- Changes a check.
-- migrate: v2
INSERT INTO orders (id, quantity) VALUES (1, 5);
-- migrate: v3
INSERT INTO orders (id, quantity) VALUES (2, 10);
INSERT INTO orders (id, quantity) VALUES (3, 11);
-- error:
