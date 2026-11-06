-- Creates tables with their keys and indexes.
-- migrate: v1
-- expect: columns(customers)
id | required
email | required

-- expect: columns(orders)
id | required
customer_id | required
state | required

-- expect: primary key(orders)
id

-- expect: indexes(customers)
ux_customers_email | unique

-- expect: indexes(orders)
ix_orders_state | index

-- expect: foreign keys(orders)
customer_id -> customers.id
