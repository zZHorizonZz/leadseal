-- The rewrites keep working after a migration renames a column that they read.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First');
-- migrate: v3
UPDATE posts SET heading = 'Changed';
-- expect: SELECT slug, revision FROM posts
changed | 1
