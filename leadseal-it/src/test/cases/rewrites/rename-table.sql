-- The rewrites keep working after a migration renames their table, and a later migration drops them.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First');
-- migrate: v4
UPDATE articles SET title = 'Second';
-- migrate: v5
UPDATE articles SET title = 'Third';
-- expect: SELECT slug, revision FROM articles
second | 1
