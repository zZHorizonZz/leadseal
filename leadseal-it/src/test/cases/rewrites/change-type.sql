-- The rewrites keep working after a migration changes the type of a column.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First');
-- migrate: v7
UPDATE posts SET title = 'Changed';
-- expect: SELECT slug, label FROM posts
changed | CHANGED
