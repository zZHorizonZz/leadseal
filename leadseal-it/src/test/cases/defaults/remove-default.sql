-- Removes the default, so an insert needs a value.
-- migrate: v5
-- migrate: v7
INSERT INTO orders (id) VALUES (1);
-- error:
