-- Makes a column required and fills its nulls.
-- migrate: v4
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v9
-- expect: columns(orders)
id | required
state | required
note | required

-- expect: SELECT note FROM orders
''
