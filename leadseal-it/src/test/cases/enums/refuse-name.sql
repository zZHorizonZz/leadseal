-- Refuses a name that is not a value of the enum.
-- migrate: v1
INSERT INTO shipments (id, state) VALUES (1, 'SENT');
INSERT INTO shipments (id, state) VALUES (2, 'LOST');
-- error:
