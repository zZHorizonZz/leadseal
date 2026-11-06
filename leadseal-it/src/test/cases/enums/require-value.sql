-- Makes an enum column required.
-- migrate: v13
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
-- migrate: v1
-- expect: columns(shipments)
id | required
state | required

INSERT INTO shipments (id, state) VALUES (2, 'LOST');
-- error:
