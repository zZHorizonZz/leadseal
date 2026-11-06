-- Refuses a number that is not a value of the enum.
-- migrate: v2
INSERT INTO shipments (id, state) VALUES (1, 1);
INSERT INTO shipments (id, state) VALUES (2, 5);
-- error:
