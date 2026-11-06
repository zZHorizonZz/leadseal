-- A rewrite replaces the value that the update writes into its column.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First');
UPDATE posts SET slug = 'custom';
-- expect: SELECT slug, revision FROM posts
first | 1
