-- Sets the current time and date, in the existing rows and in new rows.
-- migrate: v1
INSERT INTO orders (id) VALUES (1);
-- migrate: v3
INSERT INTO orders (id) VALUES (2);
-- expect: SELECT COUNT(*) FROM orders WHERE created IS NOT NULL AND day IS NOT NULL
2
