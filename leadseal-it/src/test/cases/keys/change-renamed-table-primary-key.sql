-- Changes the primary key of a table that the same migration renames.
-- migrate: v1
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v15
INSERT INTO purchases VALUES (1, 1, 2);
-- expect: primary key(purchases)
id
line
