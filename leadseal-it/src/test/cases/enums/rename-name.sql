-- Renames a value of an enum that stores names, in the rows too.
-- databases: postgresql
-- migrate: v1
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v15
-- expect: SELECT state FROM shipments
SHIPPED
