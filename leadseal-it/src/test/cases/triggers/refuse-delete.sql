-- An assert refuses a delete with its message, and lets the other rows go.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'PAID'), (2, 'NEW');
DELETE FROM orders WHERE id = 1;
-- error: Paid orders stay.
DELETE FROM orders WHERE id = 2;
-- expect: SELECT id FROM orders
1
