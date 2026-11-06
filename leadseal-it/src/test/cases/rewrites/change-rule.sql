-- A migration that changes the rewrites runs the new ones.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First');
-- migrate: v2
UPDATE posts SET title = 'Changed';
-- expect: SELECT slug, revision FROM posts
CHANGED | 0
