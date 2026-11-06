-- A later migration drops the triggers.
-- migrate: v1
-- migrate: v6
INSERT INTO orders (id, state) VALUES (1, 'PAID');
DELETE FROM orders;
-- expect: SELECT order_id FROM order_events
