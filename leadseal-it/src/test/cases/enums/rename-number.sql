-- Renames a value of an enum that stores numbers, which keeps the rows.
-- migrate: v2
INSERT INTO shipments (id, state) VALUES (1, 1);
-- migrate: v14
-- expect: SELECT state FROM shipments
1
