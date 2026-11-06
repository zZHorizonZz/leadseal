-- Fills the rows of a table with the defaults of new columns.
-- migrate: v1
INSERT INTO orders (id) VALUES (1);
-- migrate: v2
-- expect: SELECT COUNT(*) FROM orders WHERE bool_value = literal(bool, 'true')
1

-- expect: SELECT COUNT(*) FROM orders WHERE int32_value = literal(int32, '42')
1

-- expect: SELECT COUNT(*) FROM orders WHERE int64_value = literal(int64, '5000000000')
1

-- expect: SELECT COUNT(*) FROM orders WHERE float_value = literal(float, '1.5')
1

-- expect: SELECT COUNT(*) FROM orders WHERE double_value = literal(double, '2.25')
1

-- expect: SELECT COUNT(*) FROM orders WHERE decimal_value = literal(decimal, '12.50')
1

-- expect: SELECT COUNT(*) FROM orders WHERE text_value = literal(string, 'it''s a \ test')
1

-- expect: SELECT COUNT(*) FROM orders WHERE bytes_value = literal(bytes, '0aff')
1

-- expect: SELECT COUNT(*) FROM orders WHERE uuid_value = literal(uuid, '123e4567-e89b-12d3-a456-426614174000')
1

-- expect: SELECT COUNT(*) FROM orders WHERE timestamp_value = literal(timestamp, '2026-10-14T07:30:00Z')
1

-- expect: SELECT COUNT(*) FROM orders WHERE date_value = literal(date, '2026-10-14')
1

-- expect: SELECT COUNT(*) FROM orders WHERE time_value = literal(time, '07:30:00')
1

-- expect: SELECT COUNT(*) FROM orders WHERE json_value = literal(json, '{"a": 1}')
1

