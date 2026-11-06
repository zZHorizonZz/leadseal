-- A column that stops being an identity column needs its values.
-- databases: postgresql, mariadb
-- migrate: v1
INSERT INTO small_customers (name) VALUES ('Ada');
INSERT INTO customers (name) VALUES ('Ada');
-- migrate: v2
INSERT INTO small_customers (id, name) VALUES (5, 'Grace');
INSERT INTO small_customers (name) VALUES ('Linus');
-- error:
-- expect: SELECT name FROM small_customers ORDER BY id
Ada
Grace

INSERT INTO customers (id, name) VALUES (5, 'Grace');
INSERT INTO customers (name) VALUES ('Linus');
-- error:
-- expect: SELECT name FROM customers ORDER BY id
Ada
Grace

