-- A column that becomes an identity column continues after the existing keys.
-- databases: postgresql, mariadb, sqlite
-- migrate: v2
INSERT INTO small_customers (id, name) VALUES (7, 'Ada');
INSERT INTO customers (id, name) VALUES (7, 'Ada');
-- migrate: v1
INSERT INTO small_customers (name) VALUES ('Grace');
-- expect: SELECT name FROM small_customers WHERE id > 7
Grace

INSERT INTO customers (name) VALUES ('Grace');
-- expect: SELECT name FROM customers WHERE id > 7
Grace

