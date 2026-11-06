-- An insert writes no rewrite: the defaults give the first values.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'Hello World');
-- expect: SELECT slug, label, revision, modified FROM posts
NULL | NULL | 0 | NULL
