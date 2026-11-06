-- Defaults to a value that the same migration adds.
-- migrate: v1
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v5
INSERT INTO shipments (id) VALUES (2);
-- expect: SELECT state FROM shipments ORDER BY id
SENT
LOST
