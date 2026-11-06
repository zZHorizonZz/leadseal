-- Generates the keys of 32-bit and 64-bit identity columns.
-- migrate: v1
INSERT INTO small_customers (name) VALUES ('Ada');
INSERT INTO small_customers (name) VALUES ('Grace');
-- expect: SELECT id FROM small_customers WHERE name = 'Grace'
2

INSERT INTO customers (name) VALUES ('Ada');
INSERT INTO customers (name) VALUES ('Grace');
-- expect: SELECT id FROM customers WHERE name = 'Grace'
2

