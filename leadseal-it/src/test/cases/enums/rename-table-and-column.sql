-- Keeps the values of an enum through renames of the table and the column.
-- migrate: v2
INSERT INTO shipments (id, state) VALUES (1, 1);
-- migrate: v8
-- migrate: v9
INSERT INTO parcels (id, status) VALUES (2, 2);
INSERT INTO parcels (id, status) VALUES (3, 5);
-- error:
