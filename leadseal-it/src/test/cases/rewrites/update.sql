-- An update writes the rewrites on every row, each after the rewrites of the columns that it reads.
-- migrate: v1
INSERT INTO posts (id, title) VALUES (1, 'First'), (2, 'Second');
UPDATE posts SET title = 'Changed Title';
-- expect: SELECT slug, label, revision FROM posts ORDER BY id
changed title | CHANGED TITLE | 1
changed title | CHANGED TITLE | 1

-- expect: SELECT COUNT(*) FROM posts WHERE modified IS NOT NULL
2
