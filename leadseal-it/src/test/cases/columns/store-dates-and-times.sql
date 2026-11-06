-- Stores dates and times.
-- migrate: v14
INSERT INTO orders (id, day, hour) VALUES (1, '2026-10-14', '07:30:15');
-- expect: SELECT COUNT(*) FROM orders WHERE day = literal(date, '2026-10-14') AND hour = literal(time, '07:30:15')
1
