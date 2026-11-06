-- Changes a foreign key column that the same migration renames.
-- migrate: v16
INSERT INTO customers VALUES (1);
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v17
-- expect: foreign keys(orders)
buyer_id -> customers.id

-- expect: columns(orders)
id | required
buyer_id | optional
line | required
