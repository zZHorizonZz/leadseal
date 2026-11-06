-- Creates a foreign key of two columns.
-- migrate: v14
INSERT INTO order_line VALUES (1, 1, 1);
INSERT INTO orders VALUES (1, 1, 1);
INSERT INTO orders VALUES (2, 1, 2);
-- error:
DELETE FROM order_line;
-- expect: SELECT COUNT(*) FROM orders
0
