-- Drops a foreign key.
-- migrate: v7
INSERT INTO customers VALUES (1);
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v3
INSERT INTO orders VALUES (2, 9, 1);
-- expect: foreign keys(orders)

-- expect: SELECT id FROM orders ORDER BY id
1
2
