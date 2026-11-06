-- Accepts a value that a later migration adds at the end.
-- migrate: v1
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v3
INSERT INTO shipments (id, state) VALUES (2, 'LOST');
-- expect: SELECT state FROM shipments ORDER BY id
SENT
LOST
