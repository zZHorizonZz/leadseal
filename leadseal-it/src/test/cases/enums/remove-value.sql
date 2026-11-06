-- Refuses a value that a later migration removes, and keeps the rows.
-- migrate: v3
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v1 allow_unsafe
INSERT INTO shipments (id, state) VALUES (2, 'LOST');
-- error:
-- expect: SELECT state FROM shipments
SENT
