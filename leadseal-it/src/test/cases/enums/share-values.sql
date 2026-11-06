-- Changes the values of an enum that two tables share.
-- migrate: v6
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
INSERT INTO returns (id, state) VALUES (1, 'NEW');
-- migrate: v7 allow_unsafe
INSERT INTO returns (id, state) VALUES (2, 'LOST');
-- error:
-- expect: SELECT state FROM shipments
SENT

-- expect: SELECT state FROM returns
NEW
