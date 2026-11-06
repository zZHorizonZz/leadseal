-- Adds a required column of each type to a table with rows, and fills it.
-- migrate: v1
INSERT INTO orders (id, state) VALUES (1, 'new');
-- migrate: v3
-- expect: SELECT COUNT(*) FROM orders WHERE bool_value IS NOT NULL AND int32_value IS NOT NULL AND int64_value IS NOT NULL AND float_value IS NOT NULL AND double_value IS NOT NULL AND decimal_value IS NOT NULL AND text_value IS NOT NULL AND bytes_value IS NOT NULL AND uuid_value IS NOT NULL AND timestamp_value IS NOT NULL AND date_value IS NOT NULL AND time_value IS NOT NULL AND json_value IS NOT NULL
1
