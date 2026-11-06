-- An update writes a row for each of its rows.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'NEW'), (2, 'NEW');
UPDATE orders SET state = 'SENT';
-- expect: SELECT order_id FROM order_events WHERE state = 'SENT' ORDER BY order_id
1
2
