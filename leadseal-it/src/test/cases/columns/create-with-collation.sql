-- Creates a column with a binary collation, so a unique index tells a from A.
-- migrate: v15
INSERT INTO orders (id, code) VALUES (1, 'a');
INSERT INTO orders (id, code) VALUES (2, 'A');
-- expect: SELECT COUNT(*) FROM orders WHERE code = 'a'
1
