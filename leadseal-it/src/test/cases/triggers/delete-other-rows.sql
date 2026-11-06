-- The actions run in their order: the delete of the events, then the count of the author.
-- migrate: v1
INSERT INTO authors (id, active) VALUES (1, '1');
INSERT INTO orders (id, state, author_id) VALUES (1, 'NEW', 1), (2, 'NEW', 1);
DELETE FROM orders WHERE id = 1;
-- expect: SELECT order_id FROM order_events
2

-- expect: SELECT orders FROM authors
1
