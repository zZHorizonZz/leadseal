-- Changes the primary key.
-- migrate: v1
INSERT INTO orders VALUES (1, 1, 1);
-- migrate: v2
INSERT INTO orders VALUES (1, 1, 2);
-- expect: primary key(orders)
id
line

INSERT INTO orders VALUES (1, 1, 2);
-- error:
