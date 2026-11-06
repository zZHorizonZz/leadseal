-- The rewrites go with their table, so a new table can take its name.
-- migrate: v1
-- migrate: v6
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First');
UPDATE posts SET title = 'Changed';
-- expect: SELECT slug FROM posts
changed
