-- Creates generated columns, which accept NULL.
-- migrate: v1
-- expect: columns(orders)
id | required
price | required
quantity | required
note | optional
total | optional
label | optional
