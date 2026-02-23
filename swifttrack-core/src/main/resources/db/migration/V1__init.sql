CREATE TABLE orders (
  id UUID PRIMARY KEY,
  client_id VARCHAR(64),
  pickup_address TEXT,
  drop_address TEXT,
  priority VARCHAR(16),
  status VARCHAR(32),
  cms_order_id VARCHAR(64),
  route_id VARCHAR(64),
  failure_reason TEXT,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
