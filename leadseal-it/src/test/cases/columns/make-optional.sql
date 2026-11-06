-- Makes a column optional.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v10
UPDATE orders SET state = NULL;
-- expect: columns(orders)
id | required
state | optional
