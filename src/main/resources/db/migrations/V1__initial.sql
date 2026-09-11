CREATE TABLE IF NOT EXISTS item_events (
  event_id BINARY(16) PRIMARY KEY,
  event_type VARCHAR(32) NOT NULL,
  timestamp BIGINT NOT NULL,
  player_uuid BINARY(16),
  world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE,
  yaw FLOAT, pitch FLOAT,
  material VARCHAR(64),
  before_json MEDIUMTEXT,
  after_json MEDIUMTEXT,
  source VARCHAR(64),
  INDEX idx_player_time (player_uuid, timestamp DESC),
  INDEX idx_time (timestamp DESC)
);

CREATE TABLE IF NOT EXISTS restorations (
  restoration_id BINARY(16) PRIMARY KEY,
  event_id BINARY(16) NOT NULL,
  admin_uuid BINARY(16) NOT NULL,
  target_uuid BINARY(16) NOT NULL,
  timestamp BIGINT NOT NULL,
  world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE,
  yaw FLOAT, pitch FLOAT,
  result_json MEDIUMTEXT,
  status VARCHAR(16),
  INDEX idx_event (event_id),
  INDEX idx_target_time (target_uuid, timestamp DESC)
);

CREATE TABLE IF NOT EXISTS schema_migrations (
  version INT PRIMARY KEY,
  applied_at BIGINT NOT NULL
);