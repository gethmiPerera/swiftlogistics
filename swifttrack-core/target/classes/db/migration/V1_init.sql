CREATE TABLE orders (
  id               UUID PRIMARY KEY,
  client_id        VARCHAR(64) NOT NULL,
  pickup_address   TEXT NOT NULL,
  drop_address     TEXT NOT NULL,
  priority         VARCHAR(16) NOT NULL,
  status           VARCHAR(32) NOT NULL,
  cms_order_id     VARCHAR(64),
  route_id         VARCHAR(64),
  failure_reason   TEXT,
  created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_events_audit (
  id         BIGSERIAL PRIMARY KEY,
  order_id   UUID NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload    TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);