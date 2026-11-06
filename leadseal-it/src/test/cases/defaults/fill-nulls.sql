-- Fills the nulls with the default when a column becomes required.
-- migrate: v8
INSERT INTO orders (id) VALUES (1);
-- migrate: v9
-- expect: SELECT state FROM orders
new
