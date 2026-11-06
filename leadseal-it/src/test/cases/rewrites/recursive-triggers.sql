-- The rewrites run once, even where the database runs triggers recursively.
-- migrate: v1
-- only: sqlserver
ALTER DATABASE CURRENT SET RECURSIVE_TRIGGERS ON;
INSERT INTO posts (id, title) VALUES (1, 'First');
UPDATE posts SET id = id;
-- expect: SELECT revision FROM posts
1
