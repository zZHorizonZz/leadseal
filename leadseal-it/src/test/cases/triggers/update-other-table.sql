-- An update of another table runs once for each row of the insert.
-- migrate: v1
INSERT INTO authors (id, active) VALUES (1, '1');
INSERT INTO orders (id, state, author_id) VALUES (1, 'NEW', 1), (2, 'NEW', 1);
-- expect: SELECT orders FROM authors
2
