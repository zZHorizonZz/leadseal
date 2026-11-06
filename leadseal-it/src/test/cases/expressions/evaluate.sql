-- Writes the value of each expression, as rewrites that an update runs.
-- migrate: v1
INSERT INTO samples (id, quantity, price, name, nickname, paid, created) VALUES (1, 7, -2.5, ' Ana ', NULL, literal(bool, 'true'), literal(timestamp, '2020-01-01T00:00:00Z'));
INSERT INTO samples (id, quantity, price, name, nickname, paid, created) VALUES (2, -3, 4.0, 'Bob', 'Bobby', literal(bool, 'false'), literal(timestamp, '2099-01-01T00:00:00Z'));
UPDATE samples SET id = id;
-- expect: SELECT positive, many_paid, unpaid_or_negative, is_paid, not_paid, paid_false, always FROM samples ORDER BY id
1 | 1 | 0 | 1 | 0 | 0 | 1
0 | 0 | 1 | 0 | 1 | 1 | 1

-- expect: SELECT sign_matches, no_nickname, has_nickname, not_bob, at_most, listed, in_none FROM samples ORDER BY id
1 | 1 | 0 | 1 | 0 | 1 | 0
1 | 0 | 1 | 0 | 1 | 0 | 0

-- expect: SELECT abs_price, abs_quantity, trimmed, upper_bob, length_five, bob_bang, coalesce_name FROM samples ORDER BY id
1 | 0 | 1 | 0 | 1 | 0 | 0
0 | 1 | 0 | 1 | 0 | 1 | 1

-- expect: SELECT coalesce_none, half_three, half_minus, mod_four, mod_two, price_half, negated FROM samples ORDER BY id
1 | 1 | 0 | 1 | 0 | 0 | 0
0 | 0 | 1 | 0 | 1 | 1 | 1

-- expect: SELECT twice, price_plus, chosen, neg_label, past, uuids FROM samples ORDER BY id
1 | 0 | 1 | 0 | 1 | 1
0 | 1 | 0 | 1 | 0 | 1

-- expect: SELECT v_arith, v_coalesce, v_length, v_half, v_choice, v_unset FROM samples WHERE id = 1
15 | none | 5 | 3 | y | unset

-- expect: SELECT COUNT(*) FROM samples WHERE id = 1 AND v_concat = ' Ana ?'
1
