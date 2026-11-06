-- Renames a value and removes another in one migration.
-- databases: postgresql
-- migrate: v3
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v15 allow_unsafe
-- expect: SELECT state FROM shipments
SHIPPED
