-- An expression reads the row that a key references, and a missing row passes.
-- migrate: v1
INSERT INTO authors (id, active) VALUES (1, '1'), (2, '0');
INSERT INTO orders (id, state, author_id) VALUES (1, 'NEW', 1);
INSERT INTO orders (id, state, author_id) VALUES (2, 'NEW', NULL);
INSERT INTO orders (id, state, author_id) VALUES (3, 'NEW', 2);
-- error: The author is not active.
-- expect: SELECT id FROM orders ORDER BY id
1
2
