-- An update that meets the condition writes a row with the rewritten values, once.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'NEW');
UPDATE orders SET total = 5;
UPDATE orders SET state = 'PAID';
-- expect: SELECT order_id, state, revision FROM order_events ORDER BY id
1 | NEW | NULL
1 | PAID | 2
