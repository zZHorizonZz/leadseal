-- Creates a table with a check.
-- migrate: v2
INSERT INTO orders (id, quantity) VALUES (1, 0);
INSERT INTO orders (id, quantity) VALUES (2, -1);
-- error:
