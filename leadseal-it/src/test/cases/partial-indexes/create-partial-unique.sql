-- A partial unique index refuses only the rows that meet its condition.
-- databases: postgresql, sqlserver, sqlite
-- migrate: v1
INSERT INTO orders VALUES (1, 1, 1);
INSERT INTO orders VALUES (2, 1, 2);
INSERT INTO orders VALUES (3, 1, 3);
INSERT INTO orders VALUES (4, 1, 1);
-- error:
