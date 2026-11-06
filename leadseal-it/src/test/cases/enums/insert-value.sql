-- Accepts a value that a later migration adds between two values.
-- migrate: v1
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v4
INSERT INTO shipments (id, state) VALUES (2, 'PACKED');
-- expect: SELECT state FROM shipments ORDER BY id
SENT
PACKED
