-- The triggers keep working after a migration changes the type of a column of their table.
-- migrate: v1
-- migrate: v5
INSERT INTO orders (id, state) VALUES (1, 'PAID');
DELETE FROM orders;
-- error: Paid orders stay.
-- expect: SELECT COUNT(*) FROM order_events
1
