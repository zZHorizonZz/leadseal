-- The triggers keep working after a migration changes their table and drops a column that they wrote.
-- migrate: v1
-- migrate: v4
INSERT INTO orders (id, state) VALUES (1, 'NEW');
UPDATE orders SET state = 'PAID';
-- expect: SELECT state FROM order_events ORDER BY id
NEW
PAID
