-- Changes a text column to an enum, and back.
-- migrate: v12
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v1 allow_unsafe
INSERT INTO shipments (id, state) VALUES (2, 'LOST');
-- error:
-- migrate: v12
INSERT INTO shipments (id, state) VALUES (3, 'LOST');
-- expect: SELECT state FROM shipments ORDER BY id
SENT
LOST
